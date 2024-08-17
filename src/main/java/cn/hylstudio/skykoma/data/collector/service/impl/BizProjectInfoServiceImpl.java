package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.*;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ProjectEntityNodeProjection;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.projection.ScanRecordEntityProjection;
import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.*;
import cn.hylstudio.skykoma.data.collector.model.payload.*;
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
import java.util.Collections;
import java.util.List;
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
    private ScanRecordEntityRepo scanRecordEntityRepo;
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
    public void updateProjectBasicInfoAsync(ProjectInfoUploadPayload payload) {
        updateProjectInfoSync(payload);
    }

    private void updateProjectInfoSync(ProjectInfoUploadPayload payload) {
        String scanId = payload.getScanId();
        ProjectInfoDto projectInfoDto = payload.getProjectInfoDto();
        String projectKey = projectInfoDto.getKey();
        ProjectEntityNodeProjection projectEntityNodeProjection = projectEntityRepo
                .findProjectEntityNodeProjectionByKey(projectKey);
        if (projectEntityNodeProjection == null) {
            throw new BizException(BizCode.NOT_FOUND, "project not exists");
        }
        ScanRecordEntity scanRecordEntity = scanRecordEntityRepo.findByScanId(scanId);
        if (scanRecordEntity == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId error");
        }
        scanRecordEntity.setScanId(scanId);
        scanRecordEntity.setStatus(ScanRecordEntity.STATUS_UPLOADING);
        scanRecordEntity = scanRecordEntityRepo.save(scanRecordEntity);
        LOGGER.info("uploadProjectInfo, update scan STATUS_UPLOADING, projectKey = [{}], scanId = [{}]", projectKey, scanId);
        // vcs info process
        long begin1 = System.currentTimeMillis();
        VCSEntityDto vcsEntityDto = projectInfoDto.getVcsEntityDto();
        String path = vcsEntityDto.getPath();
        VCSEntity vcsEntity = vcsEntityRepo.findByPath(path);
        if (vcsEntity == null) {
            vcsEntity = new VCSEntity(vcsEntityDto);
            LOGGER.info("uploadProjectInfo, vcsEntity not exists, gen new VCSEntity = [{}]", vcsEntity);
        }
        String vcsEntityId = vcsEntity.getId();
        String projectEntityId = projectEntityNodeProjection.getId();
        projectEntityRepo.updateVcsEntity(projectEntityId, vcsEntityId);
        long dur1 = System.currentTimeMillis() - begin1;
        LOGGER.info("uploadProjectInfo, saved vcsEntity, projectKey = [{}], scanId = [{}], dur = {}ms", projectKey, scanId, dur1);

        // scanRecord process
        long begin2 = System.currentTimeMillis();
        List<ModuleDto> moduleDtos = projectInfoDto.getModules();
        FileDto rootFolderDto = projectInfoDto.getRootFolder();
        FileEntity rootFolder = new FileEntity(rootFolderDto);
        scanRecordEntity.setRootFolder(rootFolder);
        List<ModuleEntity> moduleEntities = saveModuleEntities(moduleDtos);
        scanRecordEntity.setModules(moduleEntities);
        String scanRecordEntityId = scanRecordEntity.getId();
        projectEntityRepo.addScanRecordRel(projectEntityId, scanRecordEntityId);
        projectEntityRepo.symbolLinkModuleRootToFileTree(scanId);
        long dur2 = System.currentTimeMillis() - begin2;
        LOGGER.info("uploadProjectInfo, saved module info, projectKey = [{}], scanId = [{}], dur = {}ms", projectKey, scanId, dur2);
        scanRecordEntity.setStatus(ScanRecordEntity.STATUS_UPLOADED);
        scanRecordEntity = scanRecordEntityRepo.save(scanRecordEntity);
        LOGGER.info("uploadProjectInfo, update scan STATUS_UPLOADED, projectKey = [{}], scanId = [{}]", projectKey, scanId);
    }

    @Async
    @Override
    public void updateProjectFileInfoAsync(ProjectFileInfoUploadPayload payload) {
        long begin = System.currentTimeMillis();
        updateProjectFileInfoSync(payload);
        long duration = System.currentTimeMillis() - begin;
        LOGGER.info("updateProjectFileInfoAsync file = [{}], scanId = [{}], dur = {}ms",
                payload.getFileDto().getName(), payload.getScanId(), duration);
    }

    @Override
    public ScanRecordDto queryScan(QueryScanPayload payload) {
        String scanId = payload.getScanId();
        ScanRecordEntityProjection scanRecordEntityProjectionByScanId = scanRecordEntityRepo.findScanRecordEntityProjectionByScanId(scanId);
        if (scanRecordEntityProjectionByScanId == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId error");
        }
        ScanRecordDto scanRecordDto = new ScanRecordDto(scanRecordEntityProjectionByScanId);
        String relativePath = payload.getRelativePath();
        if (StringUtils.hasText(relativePath)) {
            FileEntity fileEntity = fileEntityRepo.findByScanIdAndRelativePath(scanId, relativePath);
            if (fileEntity == null) {
                throw new BizException(BizCode.WRONG_PARAMS, "relativePath error");
            }
            String status = fileEntity.getScanStatus();
            scanRecordDto.setStatus(status);
        }
        return scanRecordDto;
    }

    @Override
    public ScanRecordDto beginScan(BeginScanPayload payload) {
        ProjectInfoDto projectInfoDto = payload.getProjectInfoDto();
        String projectKey = projectInfoDto.getKey();
        ProjectEntityNodeProjection projectEntityNodeProjection = projectEntityRepo.findProjectEntityNodeProjectionByKey(projectKey);
        if (projectEntityNodeProjection == null) {
            ProjectEntity projectEntity = new ProjectEntity(projectInfoDto);
            projectEntity = projectEntityRepo.save(projectEntity);
            projectEntityNodeProjection = projectEntity.toProjection();
        }

        String scanId = payload.getScanId();
        ScanRecordEntityProjection scanRecordEntityProj = scanRecordEntityRepo.findScanRecordEntityProjectionByScanId(scanId);
        if (scanRecordEntityProj != null) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId error");
        }
        ScanRecordEntity scanRecordEntity = new ScanRecordEntity();
        scanRecordEntity.setScanId(scanId);
        scanRecordEntity.setStatus(ScanRecordEntity.STATUS_INIT);
        scanRecordEntity = scanRecordEntityRepo.save(scanRecordEntity);

        String projectEntityId = projectEntityNodeProjection.getId();
        String scanRecordEntityId = scanRecordEntity.getId();
        projectEntityRepo.addScanRecordRel(projectEntityId, scanRecordEntityId);
        ScanRecordDto scanRecordDto = new ScanRecordDto(scanRecordEntity);
        return scanRecordDto;
    }

    @Override
    public ScanRecordDto updateScanStatus(UpdateScanStatusPayload payload) {
        String scanId = payload.getScanId();
        ScanRecordEntityProjection scanRecordEntityProjectionByScanId = scanRecordEntityRepo.findScanRecordEntityProjectionByScanId(scanId);
        if (scanRecordEntityProjectionByScanId == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId error");
        }
        String status = payload.getStatus();
        scanRecordEntityRepo.updateStatus(scanId, status);
        scanRecordEntityProjectionByScanId = scanRecordEntityRepo.findScanRecordEntityProjectionByScanId(scanId);
        ScanRecordDto scanRecordDto = new ScanRecordDto(scanRecordEntityProjectionByScanId);
        String relativePath = payload.getRelativePath();
        if (StringUtils.hasText(relativePath)) {
            FileEntity fileEntity = fileEntityRepo.findByScanIdAndRelativePath(scanId, relativePath);
            if (fileEntity == null) {
                throw new BizException(BizCode.WRONG_PARAMS, "relativePath error");
            }
            fileEntity.setScanStatus(status);
            fileEntityRepo.updateScanStatus(scanId, fileEntity.getId(), status);
            scanRecordDto.setStatus(status);
        }
        return scanRecordDto;
    }

    private void updateProjectFileInfoSync(ProjectFileInfoUploadPayload payload) {
        String scanId = payload.getScanId();
        FileDto fileDto = payload.getFileDto();
        String psiFileJson = fileDto.getPsiFileJson();
        if (!StringUtils.hasText(psiFileJson)) {
            LOGGER.info("updateProjectFileInfoSync, psiFileJson empty, scanId = [{}]", scanId);
            return;
        }
        ScanRecordEntityProjection scanRecordEntityProjectionByScanId = scanRecordEntityRepo.findScanRecordEntityProjectionByScanId(scanId);
        if (scanRecordEntityProjectionByScanId == null) {
            LOGGER.info("updateProjectFileInfoSync, scanRecord not found, scanId = [{}]", scanId);
            return;
        }
        String relativePath = fileDto.getRelativePath();
        FileEntity fileEntity = fileEntityRepo.findByScanIdAndRelativePath(scanId, relativePath);
        if (fileEntity == null) {
            LOGGER.info("updateProjectFileInfoSync, fileEntity not found, scanId = [{}], relativePath = [{}]", scanId, relativePath);
            return;
        }
        String fileEntityId = fileEntity.getId();
        fileEntityRepo.updateScanStatus(scanId, fileEntityId, ScanRecordEntity.STATUS_SCANNING);
        List<PsiElementEntity> psiElementRoots = processPsiFileJson(scanId, fileDto, fileEntity, psiFileJson);
        psiElementRoots = psiElementEntityRepo.saveAll(psiElementRoots);
        List<String> psiElementIds = psiElementRoots.stream().map(PsiElementEntity::getId).collect(Collectors.toList());
        psiElementEntityRepo.attachToFileEntity(scanId, fileEntityId, psiElementIds);
//        scanRecordEntityRepo.updateStatus(scanId, ScanRecordEntity.STATUS_SCANNED);
        fileEntityRepo.updateScanStatus(scanId, fileEntityId, ScanRecordEntity.STATUS_SCANNED);
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

//    private void calculateRelations(ScanRecordEntity scanRecordEntity, ProjectInfoUploadPayload payload) {
//        String scanId = payload.getScanId();
//        ProjectInfoDto projectInfoDto = payload.getProjectInfoDto();
//        FileDto rootFolder = projectInfoDto.getRootFolder();
//        // TODO 插件扩展
//        long begin = System.currentTimeMillis();
//        LOGGER.info("calculateRelations, scanPsiFiles begin, scanId = [{}]", scanId);
//        List<FileDto> psiFiles = scanFileRecursively(new ArrayList<>(), rootFolder,
//                v -> StringUtils.hasText(v.getPsiFileJson()));
//        scanPsiFiles(scanId, psiFiles);
//        long duration = System.currentTimeMillis() - begin;
//        LOGGER.info("calculateRelations, scanPsiFiles end, scanId = [{}], duration = {}ms", scanId, duration);
//    }

//    private void scanPsiFiles(String scanId, List<FileDto> psiFiles) {
//        int size = psiFiles.size();
//        AtomicInteger count = new AtomicInteger(1);
//        //stage1 生成语法树和类型信息树
//        psiFiles.parallelStream().forEach(fileDto -> {
//            long begin = System.currentTimeMillis();
//            String psiFileJson = fileDto.getPsiFileJson();
//            List<PsiElementEntity> psiElementRoots = processPsiFileJson(scanId, fileDto, psiFileJson);
//            FileEntity fileEntity = fileDto.getFileEntity();
//            String fileEntityId = fileEntity.getId();
//            psiElementRoots = psiElementEntityRepo.saveAll(psiElementRoots);
//            List<String> psiElementIds = psiElementRoots.stream().map(PsiElementEntity::getId).collect(Collectors.toList());
//            psiElementEntityRepo.attachToFileEntity(scanId, fileEntityId, psiElementIds);
//            long duration = System.currentTimeMillis() - begin;
//            LOGGER.info("scanPsiFiles stage1 {}/{}, path = [{}], psiFileJson.length = [{}], duration = {}ms",
//                    count.getAndIncrement(), size, fileDto.getRelativePath(), psiFileJson.length(), duration);
//        });
//        //stage2 存类的信息树，防止节点重复
//        long begin = System.currentTimeMillis();
//        Collection<ClassEntity> classEntities = ClassEntity.getAllClassEntity(scanId);
//        LOGGER.info("scanPsiFiles stage2, saving classEntities, scanId = [{}], size = [{}]", scanId, classEntities.size());
//        classEntities = classEntityRepo.saveAll(classEntities);
//        long duration = System.currentTimeMillis() - begin;
//        LOGGER.info("scanPsiFiles stage2, scanId = [{}], duration = {}ms", scanId, duration);
//        //stage3 类的信息关联到当前扫描记录
//        begin = System.currentTimeMillis();
//        LOGGER.info("scanPsiFiles stage3, attachClassEntityToScanRecord begin, scanId = [{}]", scanId);
//        classEntityRepo.attachClassEntityToScanRecord(scanId);
//        duration = System.currentTimeMillis() - begin;
//        LOGGER.info("scanPsiFiles stage3, attachClassEntityToScanRecord end, scanId = [{}], duration = {}ms", scanId, duration);
//        //stage4 psiType=Annotation的alias为AnnotationEntity
//        begin = System.currentTimeMillis();
//        LOGGER.info("scanPsiFiles stage4, aliasElementsEntity begin, scanId = [{}]", scanId);
//        psiElementEntityRepo.aliasElementsAnnotationEntity(scanId);
//        //stage4 psiType=Field的alias为FieldEntity
//        psiElementEntityRepo.aliasElementsFieldEntity(scanId);
//        duration = System.currentTimeMillis() - begin;
//        LOGGER.info("scanPsiFiles stage4, aliasElementsEntity end, scanId = [{}], duration = {}ms", scanId, duration);
//        //stage5 注解和它关联的对象简化后续查询
//        begin = System.currentTimeMillis();
//        LOGGER.info("scanPsiFiles stage5, connectAllAnnotations begin, scanId = [{}]", scanId);
//        psiElementEntityRepo.connectAllAnnotations(scanId);
//        duration = System.currentTimeMillis() - begin;
//        LOGGER.info("scanPsiFiles stage5, connectAllAnnotations end, scanId = [{}], duration = {}ms", scanId, duration);
//        //stage6 连接方法上的Api入口并关联到当前扫描记录
//        begin = System.currentTimeMillis();
//        LOGGER.info("scanPsiFiles stage6, connectMethodToApiEndpoint begin, scanId = [{}]", scanId);
//        psiElementEntityRepo.connectMethodToApiEndpoint(scanId);
//        duration = System.currentTimeMillis() - begin;
//        LOGGER.info("scanPsiFiles stage6, connectMethodToApiEndpoint end, scanId = [{}], duration = {}ms", scanId, duration);
//    }

    private List<PsiElementEntity> processPsiFileJson(String scanId, FileDto file, FileEntity fileEntity, String psiFileJson) {
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
        parseBasicInfo(psiElementEntity, v);
        psiElementEntity.setChildElements(Collections.emptyList());
        JsonArray childElements = v.get("childElements").getAsJsonArray();
        int childSize = 0;
        if (childElements != null && childElements.size() > 0) {
            childSize = childElements.size();
            List<PsiElementEntity> tmp = new ArrayList<>(childSize);
            psiElementEntity.setChildElements(tmp);
            for (JsonElement childElement : childElements) {
                tmp.add(convertToPsiElementEntity(scanId, childElement));
            }
        }
        return psiElementEntity;
    }

    private static void parseBasicInfo(PsiElementEntity psiElementEntity, JsonObject v) {
        Integer elemDepth = 0;
        JsonElement psiTypeObj = v.get("elemDepth");
        if (psiTypeObj != null) {
            elemDepth = psiTypeObj.getAsInt();
        }
        Integer propDepth = 0;
        JsonElement propDepthObj = v.get("propDepth");
        if (propDepthObj != null) {
            propDepth = propDepthObj.getAsInt();
        }
        String className = "unknown";
        JsonElement classNameObj = v.get("className");
        if (classNameObj != null) {
            className = classNameObj.getAsString();
        }
        String containingFileName = "unknown";
        JsonElement containingFileNameObj = v.get("containingFileName");
        if (containingFileNameObj != null) {
            containingFileName = containingFileNameObj.getAsString();
        }
        String originText = "unknown";
        JsonElement originTextObj = v.get("originText");
        if (originTextObj != null) {
            originText = originTextObj.getAsString();
        }
        Integer lineNumber = 0;
        JsonElement lineNumberObj = v.get("lineNum");
        if (lineNumberObj != null) {
            lineNumber = lineNumberObj.getAsInt();
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
        String error = "unknown";
        JsonElement errorObj = v.get("error");
        if (errorObj != null) {
            error = errorObj.getAsString();
        }
        Boolean inProject = null;
        JsonElement inProjectObj = v.get("inProject");
        if (inProjectObj != null) {
            inProject = inProjectObj.getAsBoolean();
        }
        String relativePath = "unknown";
        JsonElement relativePathObj = v.get("relativePath");
        if (relativePathObj != null) {
            relativePath = relativePathObj.getAsString();
        }
        String absolutePath = "unknown";
        JsonElement absolutePathObj = v.get("absolutePath");
        if (absolutePathObj != null) {
            absolutePath = absolutePathObj.getAsString();
        }
        psiElementEntity.setElemDepth(elemDepth);
        psiElementEntity.setPropDepth(propDepth);
        psiElementEntity.setClassName(className);
        psiElementEntity.setContainingFileName(containingFileName);
        psiElementEntity.setOriginText(originText);
        psiElementEntity.setLineNumber(lineNumber);
        psiElementEntity.setStartOffset(startOffset);
        psiElementEntity.setEndOffset(endOffset);
        psiElementEntity.setError(error);
        psiElementEntity.setInProject(inProject);
        psiElementEntity.setRelativePath(relativePath);
        psiElementEntity.setAbsolutePath(absolutePath);
    }

}
