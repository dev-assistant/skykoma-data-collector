package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.SkykomaConstants;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.*;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ProjectEntityNodeProjection;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ScanRecordEntityProjection;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
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
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private Gson gson;

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
        ProjectEntityNodeProjection projectEntityNodeProjection = projectEntityRepo
                .findProjectEntityNodeProjectionByKey(projectKey);
        if (projectEntityNodeProjection == null) {
            ProjectEntity entity = new ProjectEntity(projectInfoDto);
            entity = projectEntityRepo.save(entity);
            projectEntityNodeProjection = new ProjectEntityNodeProjection(entity);
            // throw new BizException(BizCode.NOT_FOUND, "project not exists");
        }
        ScanRecordEntityProjection scanRecordEntityProjection = scanEntityRepo
                .findScanRecordEntityProjectionByScanId(scanId);
        ScanRecordEntity scanRecordEntity = null;
        if (scanRecordEntityProjection == null) {
            scanRecordEntity = new ScanRecordEntity();
            scanRecordEntity.setScanId(scanId);
            scanRecordEntity.setStatus(ScanRecordEntity.STATUS_UPLOAD);
            LOGGER.info("uploadProjectInfo, scanRecord not exists, gen new ScanEntity = [{}]", scanRecordEntity);
        }
        scanRecordEntity = scanEntityRepo.save(scanRecordEntity);
        VCSEntityDto vcsEntityDto = projectInfoDto.getVcsEntityDto();
        if (vcsEntityDto == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "vcsEntityDto empty");
        }
        String path = vcsEntityDto.getPath();
        // vcs info process
        VCSEntity vcsEntity = vcsEntityRepo.findByPath(path);
        if (vcsEntity == null) {
            vcsEntity = new VCSEntity(vcsEntityDto);
            LOGGER.info("uploadProjectInfo, vcsEntity not exists, gen new VCSEntity = [{}]", vcsEntity);
        }
        String vcsEntityId = vcsEntity.getId();
        String projectEntityId = projectEntityNodeProjection.getId();
        projectEntityRepo.updateVcsEntity(projectEntityId, vcsEntityId);
        scanRecordEntity.setStatus(ScanRecordEntity.STATUS_SCANNING);
        scanEntityRepo.updateStatus(scanId, ScanRecordEntity.STATUS_FINISHED);
        // scanRecord process
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
        scanEntityRepo.updateStatus(scanId, ScanRecordEntity.STATUS_FINISHED);
        scanRecordEntity.setStatus(ScanRecordEntity.STATUS_FINISHED);
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
            LOGGER.info("saveModuleEntities finished {}/{}, name = [{}], duration = {}ms", i + 1, moduleDtos.size(),
                    name, duration);
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
        // TODO 插件扩展
        long begin = System.currentTimeMillis();
        LOGGER.info("calculateRelations, scanPsiFiles begin, scanId = [{}]", scanId);
        List<FileDto> psiFiles = scanFileRecursively(new ArrayList<>(), rootFolder,
                v -> StringUtils.hasText(v.getPsiFileJson()));
        scanPsiFiles(scanId, psiFiles);
        long duration = System.currentTimeMillis() - begin;
        LOGGER.info("calculateRelations, scanPsiFiles end, scanId = [{}], duration = {}ms", scanId, duration);

        begin = System.currentTimeMillis();
        LOGGER.info("calculateRelations, connectClassAndMethodAnnotations begin, scanId = [{}]", scanId);
        psiElementEntityRepo.connectClassAndMethodAnnotations(scanId);
        duration = System.currentTimeMillis() - begin;
        LOGGER.info("calculateRelations, connectClassAndMethodAnnotations end, scanId = [{}], duration = {}ms", scanId,
                duration);

        begin = System.currentTimeMillis();
        LOGGER.info("calculateRelations, connectMethodToApiEndpoint begin, scanId = [{}]", scanId);
        psiElementEntityRepo.connectMethodToApiEndpoint(scanId);
        duration = System.currentTimeMillis() - begin;
        LOGGER.info("calculateRelations, connectMethodToApiEndpoint end, scanId = [{}], duration = {}ms", scanId,
                duration);
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
            LOGGER.info("processPsiFileJson fileEntity not found, scanId = [{}], path = [{}]", scanId,
                    file.getRelativePath());
            return;
        }
        String fileEntityId = fileEntity.getId();
        JsonElement psiFile = JsonParser.parseString(psiFileJson);
        JsonArray rootElements = psiFile.getAsJsonArray();
        List<PsiElementEntity> psiElementRoots = new ArrayList<>(rootElements.size());
        for (JsonElement rootElement : rootElements) {
            PsiElementEntity psiElement = convertToPsiElementEntity(scanId, rootElement);
            psiElementRoots.add(psiElement);
        }
        psiElementRoots = psiElementEntityRepo.saveAll(psiElementRoots);
        List<String> psiElementIds = psiElementRoots.stream().map(PsiElementEntity::getId).collect(Collectors.toList());
        psiElementEntityRepo.attachToFileEntity(scanId, fileEntityId, psiElementIds);
    }

    private PsiElementEntity convertToPsiElementEntity(String scanId, JsonElement psiElement) {
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
                tmp.add(convertToPsiElementEntity(scanId, childElement));
            }
        }
        parseBasicInfo(psiElementEntity, v);
        String psiType = psiElementEntity.getPsiType();
        if (SkykomaConstants.PSI_ELEMENT_TYPE_CLASS.equals(psiType)) {
            String qualifiedName = v.get("qualifiedName").getAsString();
            psiElementEntity.setQualifiedName(qualifiedName);
            ClassEntity classEntity = parseClassEntity(v);
            if(classEntity != null){
                psiElementEntity.setClassInfo(classEntity);
            }
        } else if (SkykomaConstants.PSI_ELEMENT_TYPE_ANNOTATION.equals(psiType)) {
            psiElementEntity.setQualifiedName(v.get("qualifiedName").getAsString());
            JsonObject annotationClassObj = v.get("annotationClass").getAsJsonObject();
            ClassEntity classEntity = parseClassEntity(annotationClassObj);
            if(classEntity != null){
                psiElementEntity.setClassInfo(classEntity);
            }
            JsonArray attributesArr = v.get("attributes").getAsJsonArray();
            ArrayList<AnnotationAttrEntity> attrs = new ArrayList<>(attributesArr.size());
            for (JsonElement attribute : attributesArr) {
                attrs.add(new AnnotationAttrEntity(attribute));
            }
            psiElementEntity.setAttrs(attrs);
        } else {

        }
        return psiElementEntity;
    }

    private static ClassEntity parseClassEntity(JsonObject v) {
        if(v==null){
            return null;
        }
        JsonElement qualifiedNameObj = v.get("qualifiedName");
        if(qualifiedNameObj == null){
            return null;
        }
        String qualifiedName = qualifiedNameObj.getAsString();
        if(!StringUtils.hasText(qualifiedName)||"unknown".equals(qualifiedName)){
            return null;
        }
        ClassEntity classEntity = new ClassEntity();
        classEntity.setQualifiedName(qualifiedName);
        String canonicalText = v.get("canonicalText").getAsString();
        classEntity.setCanonicalText(canonicalText);
        Boolean isInterface = v.get("isInterface").getAsBoolean();
        classEntity.setIsInterface(isInterface);
        JsonArray superTypeCanonicalTextsArr = v.get("superTypeCanonicalTexts").getAsJsonArray();
        List<String> superTypeCanonicalTexts = new ArrayList<>(superTypeCanonicalTextsArr.size());
        for (JsonElement e : superTypeCanonicalTextsArr) {
            superTypeCanonicalTexts.add(e.getAsString());
        }
        classEntity.setSuperTypeCanonicalTexts(superTypeCanonicalTexts);
        if (isInterface) {
            JsonArray extendsClassListArr = v.get("extendsClassList").getAsJsonArray();
            JsonArray extendsCanonicalTextsListArr = v.get("extendsCanonicalTextsList").getAsJsonArray();
            JsonArray extendsSuperTypeCanonicalTextsListArr = v.get("extendsSuperTypeCanonicalTextsList")
                    .getAsJsonArray();
            List<ClassEntity> extendsClassList = new ArrayList<>(extendsClassListArr.size());
            List<String> extendsCanonicalTextsList = new ArrayList<>(extendsCanonicalTextsListArr.size());
            List<String> extendsSuperTypeCanonicalTextsList = new ArrayList<>(
                    extendsSuperTypeCanonicalTextsListArr.size());
            for (JsonElement e : extendsClassListArr) {
                ClassEntity extendClassEntity = parseClassEntity(e.getAsJsonObject());
                if(extendClassEntity!=null){
                    extendsClassList.add(extendClassEntity);
                }
            }
            List<ClassEntityParentRel> extendsListRels = extendsClassList.stream()
                    .map(vv -> new ClassEntityParentRel(vv, "extend")).collect(Collectors.toList());
            for (JsonElement e : extendsCanonicalTextsListArr) {
                extendsCanonicalTextsList.add(e.getAsString());
            }
            for (JsonElement e : extendsSuperTypeCanonicalTextsListArr) {
                extendsSuperTypeCanonicalTextsList.add(e.getAsString());
            }
            classEntity.setParents(extendsListRels);
            classEntity.setExtendsCanonicalTextsList(extendsCanonicalTextsList);
            classEntity.setExtendsSuperTypeCanonicalTextsList(extendsSuperTypeCanonicalTextsList);
        } else {
            ClassEntityParentRel extendRel = null;
            JsonElement superClassObj = v.get("superClass");
            if (superClassObj != null) {
                ClassEntity superClassEntity = parseClassEntity(superClassObj.getAsJsonObject());
                if(superClassEntity!=null){
                    extendRel = new ClassEntityParentRel(superClassEntity, "extend");
                }
            }
            JsonArray implementsListArr = v.get("implementsList").getAsJsonArray();
            JsonArray implementsCanonicalTextsListArr = v.get("implementsCanonicalTextsList").getAsJsonArray();
            JsonArray implementsSuperTypeCanonicalTextsListArr = v.get("implementsSuperTypeCanonicalTextsList")
                    .getAsJsonArray();
            List<ClassEntity> implementsList = new ArrayList<>(implementsListArr.size());
            List<String> implementsCanonicalTextsList = new ArrayList<>(implementsCanonicalTextsListArr.size());
            List<String> implementsSuperTypeCanonicalTextsList = new ArrayList<>(
                    implementsSuperTypeCanonicalTextsListArr.size());
            for (JsonElement e : implementsListArr) {
                ClassEntity implementClassEntity = parseClassEntity(e.getAsJsonObject());
                if(implementClassEntity != null){
                    implementsList.add(implementClassEntity);
                }
            }
            List<ClassEntityParentRel> implementListRels = implementsList.stream()
                    .map(vv -> new ClassEntityParentRel(vv, "implement")).collect(Collectors.toList());
            for (JsonElement e : implementsCanonicalTextsListArr) {
                implementsCanonicalTextsList.add(e.getAsString());
            }
            for (JsonElement e : implementsSuperTypeCanonicalTextsListArr) {
                implementsSuperTypeCanonicalTextsList.add(e.getAsString());
            }
            List<ClassEntityParentRel> parents = new ArrayList<>(implementsList.size() + 1);
            if (extendRel != null) {
                parents.add(extendRel);
            }
            parents.addAll(implementListRels);
            classEntity.setParents(parents);
            classEntity.setImplementsCanonicalTextsList(implementsCanonicalTextsList);
            classEntity.setImplementsSuperTypeCanonicalTextsList(implementsSuperTypeCanonicalTextsList);
        }
        return classEntity;
    }

    private static void parseBasicInfo(PsiElementEntity psiElementEntity, JsonObject v) {
        psiElementEntity.setPsiType(v.get("psiType").getAsString());
        psiElementEntity.setClassName(v.get("className").getAsString());
        psiElementEntity.setStartOffset(v.get("startOffset").getAsInt());
        psiElementEntity.setEndOffset(v.get("endOffset").getAsInt());
        psiElementEntity.setOriginText(v.get("originText").getAsString());
    }

}
