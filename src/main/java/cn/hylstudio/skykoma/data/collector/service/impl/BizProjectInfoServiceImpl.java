package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.*;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ProjectEntityNodeProjection;
import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.*;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;
import cn.hylstudio.skykoma.data.collector.repo.neo4j.*;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class BizProjectInfoServiceImpl implements IBizProjectInfoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BizProjectInfoServiceImpl.class);
    @Autowired
    private ProjectEntityRepo projectEntityRepo;
    @Autowired
    private Neo4jTemplate neo4jTemplate;
    @Autowired
    private FileEntityRepo fileEntityRepo;
    @Autowired
    private ModuleEntityRepo moduleEntityRepo;
    @Autowired
    private VcsEntityRepo vcsEntityRepo;
    @Autowired
    private ScanEntityRepo scanEntityRepo;

    @Override
    public ProjectInfoDto queryProject(ProjectInfoQueryPayload payload) {
        String projectKey = payload.getKey();
        if (!StringUtils.hasText(projectKey)) {
            throw new BizException(BizCode.WRONG_PARAMS, "empty projectKey");
        }
        String projectName = payload.getName();
        if (!StringUtils.hasText(projectName)) {
            throw new BizException(BizCode.WRONG_PARAMS, "empty projectName");
        }
        ProjectEntityNodeProjection entityDto = projectEntityRepo.findProjectEntityNodeProjectionByKey(projectKey);
        if (entityDto != null) {
            return new ProjectInfoDto(entityDto);
        }
        if (!payload.getCreateIfNotExists()) {
            throw new BizException(BizCode.NOT_FOUND, "project not exists");
        }
        ProjectEntity entity = new ProjectEntity(payload);
        entity = projectEntityRepo.save(entity);
        return new ProjectInfoDto(entity);
    }

    @Async
    @Override
    public void updateProjectInfoAsync(ProjectInfoUploadPayload payload) {
        updateProjectInfoSync(payload);
    }

    @Override
    public void updateProjectInfoSync(ProjectInfoUploadPayload payload) {
        String scanId = payload.getScanId();
        ProjectInfoDto projectInfoDto = payload.getProjectInfoDto();
        String projectKey = projectInfoDto.getKey();
        ProjectEntityNodeProjection projectEntityNodeProjection = projectEntityRepo.findProjectEntityNodeProjectionByKey(projectKey);
        if (projectEntityNodeProjection == null) {
            ProjectEntity entity = new ProjectEntity(projectInfoDto);
            entity = projectEntityRepo.save(entity);
            projectEntityNodeProjection = new ProjectEntityNodeProjection(entity);
//            throw new BizException(BizCode.NOT_FOUND, "project not exists");
        }
        ScanRecordEntity scanRecordEntity = scanEntityRepo.findByScanId(scanId);
        if (scanRecordEntity == null) {
            scanRecordEntity = new ScanRecordEntity();
            scanRecordEntity.setScanId(scanId);
            LOGGER.info("uploadProjectInfo, scanRecord not exists, gen new ScanEntity = [{}]", scanRecordEntity);
        }
        VCSEntityDto vcsEntityDto = projectInfoDto.getVcsEntityDto();
        if (vcsEntityDto == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "vcsEntityDto empty");
        }
        String path = vcsEntityDto.getPath();
        //vcs info process
        VCSEntity vcsEntity = vcsEntityRepo.findByPath(path);
        if (vcsEntity == null) {
            vcsEntity = new VCSEntity(vcsEntityDto);
            LOGGER.info("uploadProjectInfo, vcsEntity not exists, gen new VCSEntity = [{}]", vcsEntity);
        }
        String vcsEntityId = vcsEntity.getId();
        String projectEntityId = projectEntityNodeProjection.getId();
        projectEntityRepo.updateVcsEntity(projectEntityId, vcsEntityId);
        //scanRecord process
        List<ModuleDto> moduleDtos = projectInfoDto.getModules();
        if (CollectionUtils.isEmpty(moduleDtos)) {
            throw new BizException(BizCode.WRONG_PARAMS, "modules empty");
        }
        FileDto rootFolderDto = projectInfoDto.getRootFolder();
        FileEntity rootFolder = new FileEntity(rootFolderDto);
        scanRecordEntity.setRootFolder(rootFolder);
        List<ModuleEntity> moduleEntities = saveModuleEntities(moduleDtos);
        scanRecordEntity.setModules(moduleEntities);
        scanRecordEntity = scanEntityRepo.save(scanRecordEntity);
        LOGGER.info("uploadProjectInfo, saved scanEntity, scanId = [{}]", scanId);
        String scanRecordEntityId = scanRecordEntity.getId();
        projectEntityRepo.addScanRecordRel(projectEntityId, scanRecordEntityId);
        LOGGER.info("uploadProjectInfo, saved projectEntity, projectId = [{}]", projectEntityId);
        calculateRelations(scanRecordEntity, payload);
    }

    private List<ModuleEntity> saveModuleEntities(List<ModuleDto> moduleDtos) {
        List<ModuleEntity> moduleEntities = new ArrayList<>(moduleDtos.size());
        for (int i = 0; i < moduleDtos.size(); i++) {
            ModuleDto moduleDto = moduleDtos.get(i);
            String name = moduleDto.getName();
            long start = System.currentTimeMillis();
            List<ModuleRootDto> roots = moduleDto.getRoots();
            List<ModuleRootRel> rels = new ArrayList<>(4);
            for (ModuleRootDto root : roots) {
                String rootType = root.getType();
                List<FileDto> rootFolders = root.getFolders();
                List<ModuleRootRel> srcRels = saveRootFolders(rootFolders, rootType);
                rels.addAll(srcRels);
            }
            ModuleEntity moduleEntity = new ModuleEntity(moduleDto);
            moduleEntity.setRoots(rels);
            moduleEntity = moduleEntityRepo.save(moduleEntity);
            long duration = System.currentTimeMillis() - start;
            LOGGER.info("saveModuleEntities finished {}/{}, name = [{}], duration = {}ms", i + 1, moduleDtos.size(), name, duration);
            moduleEntities.add(moduleEntity);
        }
        return moduleEntities;
    }

    private List<ModuleRootRel> saveRootFolders(List<FileDto> roots, String type) {
        List<ModuleRootRel> rels = new ArrayList<>(roots.size());
        for (int i = 0; i < roots.size(); i++) {
            long start = System.currentTimeMillis();
            FileDto fileDto = roots.get(i);
            String relativePath = fileDto.getRelativePath();
            FileEntity fileEntity = saveFileDtoHierarchyRecursively(fileDto);
            ModuleRootRel rel = new ModuleRootRel(fileEntity, type);
            rels.add(rel);
            long duration = System.currentTimeMillis() - start;
            LOGGER.info("saveRootFolders finished  {}/{}, type = [{}], relativePath = [{}], duration = {}ms",
                    i + 1, roots.size(), type, relativePath, duration);
        }
        return rels;
    }

    public FileEntity saveFileDtoHierarchyRecursively(FileDto fileDto) {
        List<FileDto> subFiles = fileDto.getSubFiles();
        if (!CollectionUtils.isEmpty(subFiles)) {
            List<FileEntity> subEntities = subFiles.stream().map(this::saveFileDtoHierarchyRecursively).toList();
            FileEntity entity = new FileEntity(fileDto);
            entity.setSubFiles(subEntities);
            return fileEntityRepo.save(entity);
        }
        FileEntity entity = new FileEntity(fileDto);
        return fileEntityRepo.save(entity);
    }

    private void calculateRelations(ScanRecordEntity scanRecordEntity, ProjectInfoUploadPayload payload) {
        String scanId = payload.getScanId();
        projectEntityRepo.symbolLinkModuleRootToFileTree(scanId);
        LOGGER.info("calculateRelations, connect module roots to folders, scanId = [{}]", scanId);
    }
}
