package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.SkykomaConstants;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.*;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ProjectEntityNodeProjection;
import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.*;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;
import cn.hylstudio.skykoma.data.collector.repo.neo4j.*;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
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
    private ClassEntityRepo classEntityRepo;
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
        ScanRecordEntity scanRecordEntity = new ScanRecordEntity();
        scanRecordEntity.setScanId(scanId);
        scanRecordEntity.setStatus(ScanRecordEntity.STATUS_UPLOAD);
        LOGGER.info("uploadProjectInfo, scanRecord not exists, gen new ScanEntity = [{}]", scanRecordEntity);
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
    }

    private void scanPsiFiles(String scanId, List<FileDto> psiFiles) {
        int size = psiFiles.size();
        AtomicInteger count = new AtomicInteger(1);
        //stage1 生成语法树和类型信息树
        psiFiles.parallelStream().forEach(fileDto -> {
            long begin = System.currentTimeMillis();
            String psiFileJson = fileDto.getPsiFileJson();
            List<PsiElementEntity> psiElementRoots = processPsiFileJson(scanId, fileDto, psiFileJson);
            FileEntity fileEntity = fileDto.getFileEntity();
            String fileEntityId = fileEntity.getId();
            psiElementRoots = psiElementEntityRepo.saveAll(psiElementRoots);
            List<String> psiElementIds = psiElementRoots.stream().map(PsiElementEntity::getId).collect(Collectors.toList());
            psiElementEntityRepo.attachToFileEntity(scanId, fileEntityId, psiElementIds);
            long duration = System.currentTimeMillis() - begin;
            LOGGER.info("scanPsiFiles stage1 {}/{}, path = [{}], psiFileJson.length = [{}], duration = {}ms",
                    count.getAndIncrement(), size, fileDto.getRelativePath(), psiFileJson.length(), duration);
        });
        //stage2 存类的信息树，防止节点重复
        long begin = System.currentTimeMillis();
        Collection<ClassEntity> classEntities = ClassEntity.getAllClassEntity(scanId);
        LOGGER.info("scanPsiFiles stage2, saving classEntities, scanId = [{}], size = [{}]", scanId, classEntities.size());
        classEntities = classEntityRepo.saveAll(classEntities);
        long duration = System.currentTimeMillis() - begin;
        LOGGER.info("scanPsiFiles stage2, scanId = [{}], duration = {}ms",scanId, duration);
        //stage3 类的信息关联到当前扫描记录
        begin = System.currentTimeMillis();
        LOGGER.info("scanPsiFiles stage3, attachClassEntityToScanRecord begin, scanId = [{}]", scanId);
        classEntityRepo.attachClassEntityToScanRecord(scanId);
        duration = System.currentTimeMillis() - begin;
        LOGGER.info("scanPsiFiles stage3, attachClassEntityToScanRecord end, scanId = [{}], duration = {}ms", scanId, duration);
        //stage4 psiType=Annotation的alias为AnnotationEntity
        begin = System.currentTimeMillis();
        LOGGER.info("scanPsiFiles stage4, aliasElementToAnnotationEntity begin, scanId = [{}]", scanId);
        psiElementEntityRepo.aliasElementToAnnotationEntity(scanId);
        duration = System.currentTimeMillis() - begin;
        LOGGER.info("scanPsiFiles stage4, aliasElementToAnnotationEntity end, scanId = [{}], duration = {}ms", scanId, duration);
        //stage5 注解和它关联的对象简化后续查询
        begin = System.currentTimeMillis();
        LOGGER.info("scanPsiFiles stage5, connectAllAnnotations begin, scanId = [{}]", scanId);
        psiElementEntityRepo.connectAllAnnotations(scanId);
        duration = System.currentTimeMillis() - begin;
        LOGGER.info("scanPsiFiles stage5, connectAllAnnotations end, scanId = [{}], duration = {}ms", scanId, duration);
        //stage6 连接方法上的Api入口并关联到当前扫描记录
        begin = System.currentTimeMillis();
        LOGGER.info("scanPsiFiles stage6, connectMethodToApiEndpoint begin, scanId = [{}]", scanId);
        psiElementEntityRepo.connectMethodToApiEndpoint(scanId);
        duration = System.currentTimeMillis() - begin;
        LOGGER.info("scanPsiFiles stage6, connectMethodToApiEndpoint end, scanId = [{}], duration = {}ms", scanId, duration);
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

    private List<PsiElementEntity> processPsiFileJson(String scanId, FileDto file, String psiFileJson) {
        String relativePath = file.getRelativePath();
        FileEntity fileEntity = fileEntityRepo.findByScanIdAndRelativePath(scanId, relativePath);
        if (fileEntity == null) {
            LOGGER.info("processPsiFileJson fileEntity not found, scanId = [{}], path = [{}]", scanId,
                    file.getRelativePath());
            return Collections.emptyList();
        }
        file.setFileEntity(fileEntity);
        JsonElement psiFile = JsonParser.parseString(psiFileJson);
        JsonArray rootElements = psiFile.getAsJsonArray();
        List<PsiElementEntity> psiElementRoots = new ArrayList<>(rootElements.size());
        for (JsonElement rootElement : rootElements) {
            PsiElementEntity psiElement = convertToPsiElementEntity(scanId, rootElement);
            psiElementRoots.add(psiElement);
        }
        return psiElementRoots;
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
            ClassEntity classEntity = parseClassEntity(scanId, v);
            if (classEntity != null) {
//                psiElementEntity.setClassInfo(classEntity);
                classEntity.mergePsiElements(new ClassEntityReferRel(psiElementEntity,"declare"));
            }
        } else if (SkykomaConstants.PSI_ELEMENT_TYPE_ANNOTATION.equals(psiType)) {
            psiElementEntity.setQualifiedName(v.get("qualifiedName").getAsString());
            JsonObject annotationClassObj = v.get("annotationClass").getAsJsonObject();
            ClassEntity classEntity = parseClassEntity(scanId, annotationClassObj);
            if (classEntity != null) {
                classEntity.mergePsiElements(new ClassEntityReferRel(psiElementEntity,"refer"));
//                psiElementEntity.setClassInfo(classEntity);
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

    private static ClassEntity parseClassEntity(String scanId, JsonObject v) {
        if (v == null) {
            return null;
        }
        JsonElement qualifiedNameObj = v.get("qualifiedName");
        if (qualifiedNameObj == null) {
            return null;
        }
        String qualifiedName = qualifiedNameObj.getAsString();
        if (!StringUtils.hasText(qualifiedName) || "unknown".equals(qualifiedName)) {
            return null;
        }
        ClassEntity classEntity = ClassEntity.generateClassEntity(scanId, qualifiedName);
//        if (StringUtils.hasText(classEntity.getQualifiedName())) {
//            return classEntity;
//        }
        classEntity.setQualifiedName(qualifiedName);
        String canonicalText = v.get("canonicalText").getAsString();
        classEntity.setCanonicalText(canonicalText);
        Boolean isInterface = v.get("isInterface").getAsBoolean();
        classEntity.setIsInterface(isInterface);
        JsonArray superTypeClassListArr = v.get("superTypeClassList").getAsJsonArray();
        JsonArray superTypeCanonicalTextsArr = v.get("superTypeCanonicalTextsList").getAsJsonArray();
        JsonArray superTypeSuperTypeCanonicalTextsArr = v.get("superTypeSuperTypeCanonicalTextsList").getAsJsonArray();
        List<ClassEntity> superTypeClassList = new ArrayList<>(superTypeClassListArr.size());
        List<String> superTypeCanonicalTextsList = new ArrayList<>(superTypeCanonicalTextsArr.size());
        List<String> superTypeSuperTypeCanonicalTexts = new ArrayList<>(superTypeSuperTypeCanonicalTextsArr.size());
        for (JsonElement e : superTypeClassListArr) {
            ClassEntity superTypeClassEntity = parseClassEntity(scanId, e.getAsJsonObject());
            if (superTypeClassEntity != null) {
                superTypeClassList.add(superTypeClassEntity);
            }
        }
        List<ClassEntityParentRel> superTypeListRels = superTypeClassList.stream()
                .map(vv -> new ClassEntityParentRel(vv, "kindOf")).toList();
        for (JsonElement e : superTypeCanonicalTextsArr) {
            superTypeCanonicalTextsList.add(e.getAsString());
        }
        for (JsonElement e : superTypeSuperTypeCanonicalTextsArr) {
            superTypeSuperTypeCanonicalTexts.add(e.getAsString());
        }
        classEntity.mergeParent(superTypeListRels);
        classEntity.setSuperTypeCanonicalTextsList(superTypeCanonicalTextsList);
        classEntity.setSuperTypeSuperTypeCanonicalTexts(superTypeSuperTypeCanonicalTexts);
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
                ClassEntity extendClassEntity = parseClassEntity(scanId, e.getAsJsonObject());
                if (extendClassEntity != null) {
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
            classEntity.mergeParent(extendsListRels);
            classEntity.setExtendsCanonicalTextsList(extendsCanonicalTextsList);
            classEntity.setExtendsSuperTypeCanonicalTextsList(extendsSuperTypeCanonicalTextsList);
        } else {
            ClassEntityParentRel extendRel = null;
            JsonElement superClassObj = v.get("superClass");
            if (superClassObj != null) {
                ClassEntity superClassEntity = parseClassEntity(scanId, superClassObj.getAsJsonObject());
                if (superClassEntity != null) {
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
                ClassEntity implementClassEntity = parseClassEntity(scanId, e.getAsJsonObject());
                if (implementClassEntity != null) {
                    implementsList.add(implementClassEntity);
                }
            }
            List<ClassEntityParentRel> implementListRels = implementsList.stream()
                    .map(vv -> new ClassEntityParentRel(vv, "implement")).toList();
            for (JsonElement e : implementsCanonicalTextsListArr) {
                implementsCanonicalTextsList.add(e.getAsString());
            }
            for (JsonElement e : implementsSuperTypeCanonicalTextsListArr) {
                implementsSuperTypeCanonicalTextsList.add(e.getAsString());
            }
            List<ClassEntityParentRel> parents = new ArrayList<>(implementsList.size() + 1);
            parents.addAll(implementListRels);
            if (extendRel != null) {
                parents.add(extendRel);
            }
            classEntity.mergeParent(parents);
            classEntity.setImplementsCanonicalTextsList(implementsCanonicalTextsList);
            classEntity.setImplementsSuperTypeCanonicalTextsList(implementsSuperTypeCanonicalTextsList);
        }
        return classEntity;
    }

    private static void parseBasicInfo(PsiElementEntity psiElementEntity, JsonObject v) {
        String psiType = "unknown";
        JsonElement psiTypeObj = v.get("psiType");
        if (psiTypeObj != null) {
            psiType = psiTypeObj.getAsString();
        }
        String className = "unknown";
        JsonElement classNameObj = v.get("className");
        if (classNameObj != null) {
            className = classNameObj.getAsString();
        }
        Integer startOffset = 0;
        JsonElement startOffsetObj = v.get("startOffset");
        if (classNameObj != null) {
            startOffset = startOffsetObj.getAsInt();
        }
        Integer endOffset = 0;
        JsonElement endOffsetObj = v.get("endOffset");
        if (endOffsetObj != null) {
            endOffset = endOffsetObj.getAsInt();
        }
        String originText = "";
        JsonElement originTextObj = v.get("originText");
        if (originTextObj != null) {
            originText = originTextObj.getAsString();
        }
        Integer lineNumber = 0;
        JsonElement lineNumberObj = v.get("lineNum");
        if (lineNumberObj != null) {
            lineNumber = lineNumberObj.getAsInt();
        }
        psiElementEntity.setPsiType(psiType);
        psiElementEntity.setClassName(className);
        psiElementEntity.setStartOffset(startOffset);
        psiElementEntity.setEndOffset(endOffset);
        psiElementEntity.setOriginText(originText);
        psiElementEntity.setLineNumber(lineNumber);
    }

}
