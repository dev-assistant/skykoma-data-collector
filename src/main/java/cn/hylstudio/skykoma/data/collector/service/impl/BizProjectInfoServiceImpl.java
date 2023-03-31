package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.*;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ProjectEntityNodeProjection;
import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.*;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;
import cn.hylstudio.skykoma.data.collector.repo.neo4j.*;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    @Autowired
    private PsiElementEntityRepo psiElementEntityRepo;

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
        LOGGER.info("calculateRelations, connect module roots to folders begin, scanId = [{}]", scanId);
        projectEntityRepo.symbolLinkModuleRootToFileTree(scanId);
        LOGGER.info("calculateRelations, connect module roots to folders end, scanId = [{}]", scanId);
        ProjectInfoDto projectInfoDto = payload.getProjectInfoDto();
        FileDto rootFolder = projectInfoDto.getRootFolder();
        //TODO 插件扩展
        long begin = System.currentTimeMillis();
        LOGGER.info("calculateRelations, scanPsiFiles begin, scanId = [{}]", scanId);
        List<FileDto> psiFiles = scanFileRecursively(new ArrayList<>(), rootFolder, v -> StringUtils.hasText(v.getPsiFileJson()));
        scanPsiFiles(scanId, psiFiles);
        long duration = System.currentTimeMillis() - begin;
        LOGGER.info("calculateRelations, scanPsiFiles end, scanId = [{}], duration = {}ms", scanId, duration);
    }

    private void scanPsiFiles(String scanId, List<FileDto> psiFiles) {
        int size = psiFiles.size();
        AtomicInteger count = new AtomicInteger(1);
        psiFiles.parallelStream().forEach(fileDto -> {
            long begin = System.currentTimeMillis();
            String psiFileJson = fileDto.getPsiFileJson();
            processPsiFileJson(scanId, fileDto, psiFileJson);
            long duration = System.currentTimeMillis() - begin;
            LOGGER.info("processPsiFileJson {}/{}, path = [{}], psiFileJson.length = [{}], duration = {}ms",
                    count.getAndIncrement(), size, fileDto.getRelativePath(), psiFileJson.length(), duration);
        });
    }

    private List<FileDto> scanFileRecursively(List<FileDto> fileDtos, FileDto file, Predicate<FileDto> predicate) {
        String type = file.getType();
        LOGGER.info("scanFileRecursively folder, file = [{}], type = [{}]", file.getName(), type);
        if (FileDto.TYPE_FOLDER.equals(type)) {
            List<FileDto> subFiles = file.getSubFiles();
            if (!CollectionUtils.isEmpty(subFiles)) {
                for (FileDto fileDto : subFiles) {
                    fileDtos = scanFileRecursively(fileDtos, fileDto, predicate);
                }
            } else {
                LOGGER.info("scanFileRecursively empty folder, file = [{}], type = [{}]", file.getName(), type);
            }
        } else if (FileDto.TYPE_FILE.equals(type)) {
            if (predicate.test(file)) {
                fileDtos.add(file);
            } else {
                LOGGER.info("scanFileRecursively skip, file = [{}], type = [{}]", file.getName(), type);
            }
        } else {
            LOGGER.info("scanFileRecursively unknown file type, file = [{}], type = [{}]", file.getName(), type);
        }
        return fileDtos;
    }

    private void processPsiFileJson(String scanId, FileDto file, String psiFileJson) {

        String relativePath = file.getRelativePath();
        FileEntity fileEntity = fileEntityRepo.findByScanIdAndRelativePath(scanId, relativePath);
        if (fileEntity == null) {
            LOGGER.info("processPsiFileJson fileEntity not found, scanId = [{}], path = [{}]", scanId, file.getRelativePath());
            return;
        }
        String fileEntityId = fileEntity.getId();
        JsonElement psiFile = JsonParser.parseString(psiFileJson);
        JsonArray rootElements = psiFile.getAsJsonArray();
        List<PsiElementEntity> psiElementRoots = new ArrayList<>(rootElements.size());
        for (JsonElement rootElement : rootElements) {
            PsiElementEntity psiElement = convertToPsiElementEntity(rootElement);
            psiElementRoots.add(psiElement);
        }
        psiElementRoots = psiElementEntityRepo.saveAll(psiElementRoots);
        List<String> psiElementIds = psiElementRoots.stream().map(PsiElementEntity::getId).collect(Collectors.toList());
        psiElementEntityRepo.attachToFileEntity(scanId, fileEntityId, psiElementIds);
    }

    private PsiElementEntity convertToPsiElementEntity(JsonElement psiElement) {
        PsiElementEntity psiElementEntity = new PsiElementEntity();
        JsonObject v = psiElement.getAsJsonObject();
        JsonArray childElements = v.get("childElements").getAsJsonArray();
        psiElementEntity.setChildElements(Collections.emptyList());
        int childSize = 0;
        if (childElements != null && childElements.size() > 0) {
            childSize = childElements.size();
            List<PsiElementEntity> tmp = new ArrayList<>(childSize);
            psiElementEntity.setChildElements(tmp);
            for (JsonElement childElement : childElements) {
                tmp.add(convertToPsiElementEntity(childElement));
            }
        }
        psiElementEntity.setClassName(v.get("className").getAsString());
        psiElementEntity.setStartOffset(v.get("startOffset").getAsInt());
        psiElementEntity.setEndOffset(v.get("endOffset").getAsInt());
        psiElementEntity.setOriginText(v.get("originText").getAsString());
        return psiElementEntity;
    }
}
