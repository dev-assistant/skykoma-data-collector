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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
        if (entityDto == null) {
            if (!payload.getCreateIfNotExists()) {
                throw new BizException(BizCode.NOT_FOUND, "project not exists");
            }
            ProjectEntity entity = new ProjectEntity(payload);
            entity = projectEntityRepo.save(entity);
            return new ProjectInfoDto(entity);
        }
        return new ProjectInfoDto(entityDto);
    }

    @Override
    public ProjectInfoDto uploadProjectInfo(ProjectInfoUploadPayload payload) {
        String scanId = payload.getScanId();
        if (!StringUtils.hasText(scanId)) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId empty");
        }
        ProjectInfoDto projectInfoDto = payload.getProjectInfoDto();
        if (projectInfoDto == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "projectInfoDto empty");
        }
        String projectKey = projectInfoDto.getKey();
        if (!StringUtils.hasText(projectKey)) {
            throw new BizException(BizCode.WRONG_PARAMS, "projectId empty");
        }
        ProjectEntityNodeProjection projectEntityNodeProjection = projectEntityRepo.findProjectEntityNodeProjectionByKey(projectKey);
        if (projectEntityNodeProjection == null) {
//            ProjectEntity entity = new ProjectEntity(projectInfoDto);
//            entity = projectEntityRepo.save(entity);
//            projectEntityNodeProj ection = new ProjectEntityNodeProjection(entity);
            throw new BizException(BizCode.NOT_FOUND, "project not exists");
        }
        ScanRecordEntity scanRecordEntity = scanEntityRepo.findByScanId(scanId);
        if (scanRecordEntity == null) {
            scanRecordEntity = new ScanRecordEntity();
            scanRecordEntity.setScanId(scanId);
            LOGGER.info("uploadProjectInfo, gen new ScanEntity = [{}]", scanRecordEntity);
        } else {
            LOGGER.info("uploadProjectInfo, reuse ScanEntity = [{}]", scanRecordEntity);
        }
        VCSEntityDto vcsEntityDto = projectInfoDto.getVcsEntityDto();
        if (vcsEntityDto == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "vcsEntityDto empty");
        }
        String path = vcsEntityDto.getPath();
        VCSEntity vcsEntity = vcsEntityRepo.findByPath(path);
        if (vcsEntity == null) {
            vcsEntity = new VCSEntity(vcsEntityDto);
            LOGGER.info("uploadProjectInfo, gen new VCSEntity = [{}]", vcsEntity);
        } else {
            LOGGER.info("uploadProjectInfo, reuse VCSEntity = [{}]", vcsEntity);
        }
        String vcsEntityId = vcsEntity.getId();
        String projectEntityId = projectEntityNodeProjection.getId();
        projectEntityRepo.updateVcsEntity(projectEntityId, vcsEntityId);

        List<ModuleDto> moduleDtos = projectInfoDto.getModules();
        if (CollectionUtils.isEmpty(moduleDtos)) {
            throw new BizException(BizCode.WRONG_PARAMS, "modules empty");
        }
        FileDto rootFolderDto = projectInfoDto.getRootFolder();
        FileEntity rootFolder = new FileEntity(rootFolderDto);
        scanRecordEntity.setRootFolder(rootFolder);
        List<ModuleEntity> moduleEntities = new ArrayList<>(moduleDtos.size());
        for (int i = 0; i < moduleDtos.size(); i++) {
            ModuleDto moduleDto = moduleDtos.get(i);
            String name = moduleDto.getName();
            long start = System.currentTimeMillis();
            List<ModuleRootRel> srcRels = saveRoots(moduleDto.getSrcRoots(), "src");
            List<ModuleRootRel> testSrcRels = saveRoots(moduleDto.getTestSrcRoots(), "testSrc");
            List<ModuleRootRel> resourcesRels = saveRoots(moduleDto.getResRoots(), "resources");
            List<ModuleRootRel> testResourcesRels = saveRoots(moduleDto.getTestResRoots(), "testResources");
            List<ModuleRootRel> rels = Stream.of(srcRels, testSrcRels, resourcesRels, testResourcesRels).flatMap(List::stream).toList();
            ModuleEntity moduleEntity = new ModuleEntity(moduleDto);
            moduleEntity.setRoots(rels);
            moduleEntity = moduleEntityRepo.save(moduleEntity);
            long duration = System.currentTimeMillis() - start;
            LOGGER.info("uploadProjectInfo, saved modules {}/{}, name = [{}], duration = [{}]", i + 1, moduleDtos.size(), name, duration);
            moduleEntities.add(moduleEntity);
        }
        scanRecordEntity.setModules(moduleEntities);
        scanRecordEntity = scanEntityRepo.save(scanRecordEntity);
        LOGGER.info("uploadProjectInfo, saved scanEntity, scanId = [{}]", scanId);
        String scanRecordEntityId = scanRecordEntity.getId();
        projectEntityRepo.addScanRecordRel(projectEntityId, scanRecordEntityId);
        LOGGER.info("uploadProjectInfo, saved projectEntity, id = [{}]", projectEntityId);
        ProjectInfoDto result = new ProjectInfoDto(projectEntityNodeProjection, scanRecordEntity);
        return result;
    }

    private List<ModuleRootRel> saveRoots(List<FileDto> roots, String type) {
        List<ModuleRootRel> rels = new ArrayList<>(roots.size());
        for (int i = 0; i < roots.size(); i++) {
            long start = System.currentTimeMillis();
            FileDto fileDto = roots.get(i);
            String relativePath = fileDto.getRelativePath();
            LOGGER.info("savingRoots, type = [{}], FileDto {}/{}, relativePath = [{}]",
                    type, i + 1, roots.size(), relativePath);
            FileEntity fileEntity = saveFileDtoHierarchyRecursively(fileDto);
            ModuleRootRel rel = new ModuleRootRel(fileEntity, type);
            rels.add(rel);
            long duration = System.currentTimeMillis() - start;
            LOGGER.info("savedRoots, type = [{}], FileDto {}/{}, relativePath = [{}], duration = {}ms",
                    type, i + 1, roots.size(), relativePath, duration);
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
}
