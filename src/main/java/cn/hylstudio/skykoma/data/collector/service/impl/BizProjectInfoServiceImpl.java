package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.*;
import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.*;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;
import cn.hylstudio.skykoma.data.collector.repo.neo4j.FileEntityRepo;
import cn.hylstudio.skykoma.data.collector.repo.neo4j.ModuleEntityRepo;
import cn.hylstudio.skykoma.data.collector.repo.neo4j.ProjectEntityRepo;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Stream;

@Service
public class BizProjectInfoServiceImpl implements IBizProjectInfoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BizProjectInfoServiceImpl.class);
    @Autowired
    private ProjectEntityRepo projectEntityRepo;
    @Autowired
    private FileEntityRepo fileEntityRepo;
    @Autowired
    private ModuleEntityRepo moduleEntityRepo;

    @Override
    public ProjectDto queryProject(ProjectInfoQueryPayload payload) {
        String projectName = payload.getProjectName();
        if (!StringUtils.hasText(projectName)) {
            throw new BizException(BizCode.WRONG_PARAMS, "empty projectName");
        }
        //TODO move to base service
        ProjectEntity entity = projectEntityRepo.findByName(projectName);
        if (entity == null) {
            if (!payload.getCreateIfNotExists()) {
                throw new BizException(BizCode.NOT_FOUND, "project not exists");
            }
            entity = new ProjectEntity();
            entity.setName(projectName);
            entity = projectEntityRepo.save(entity);
        }
        return new ProjectDto(entity);
    }

    @Override
    public ProjectDto uploadProjectInfo(ProjectInfoUploadPayload payload) {
        String projectId = payload.getId();
        if (!StringUtils.hasText(projectId)) {
            throw new BizException(BizCode.WRONG_PARAMS, "projectId empty");
        }
        ProjectEntity entity = projectEntityRepo.findById(projectId).orElse(null);
        if (entity == null) {
//            throw new BizException(BizCode.NOT_FOUND, "project not exists");
            entity = new ProjectEntity(payload);
        }
        String projectName = payload.getName();
//        boolean hasUpdate = false;
//        String oldName = entity.getName();
//        if (!oldName.equals(projectName)) {
        entity.setName(projectName);
//            hasUpdate = true;
//        }
        VCSEntityDto vcsEntityDto = payload.getVcsEntityDto();
//        if (vcsEntityDto != null) {
//        TODO check
        VCSEntity vcsEntity = new VCSEntity(vcsEntityDto);
        entity.setVcsEntity(vcsEntity);
//            hasUpdate = true;
//        }
        List<ModuleDto> moduleDtos = payload.getModules();
//        if (!CollectionUtils.isEmpty(moduleDtos)) {

        List<ModuleEntity> modules = moduleDtos.stream().map(moduleDto -> {
            List<ModuleRootRel> srcRels = moduleDto.getSrcRoots().stream()
                    .map(this::saveFileDtoHierarchy)
                    .map(v -> new ModuleRootRel(v, "src")).toList();
            List<ModuleRootRel> testSrcRels = moduleDto.getTestSrcRoots().stream()
                    .map(this::saveFileDtoHierarchy)
                    .map(v -> new ModuleRootRel(v, "testSrc")).toList();
            List<ModuleRootRel> resourcesRels = moduleDto.getResRoots().stream()
                    .map(this::saveFileDtoHierarchy)
                    .map(v -> new ModuleRootRel(v, "resources")).toList();
            List<ModuleRootRel> testResourcesRels = moduleDto.getTestResRoots().stream()
                    .map(this::saveFileDtoHierarchy)
                    .map(v -> new ModuleRootRel(v, "testResources")).toList();
            List<ModuleRootRel> rels = Stream.of(srcRels, testSrcRels, resourcesRels, testResourcesRels).flatMap(List::stream).toList();
            ModuleEntity moduleEntity = new ModuleEntity(moduleDto);
            moduleEntity.setRoots(rels);
            return moduleEntityRepo.save(moduleEntity);
        }).toList();
        entity.setModules(modules);
//            hasUpdate = true;
//        }
//        if (hasUpdate) {
        entity = projectEntityRepo.save(entity);
//        }

        ProjectDto result = new ProjectDto();
        return result;
    }

    public FileEntity saveFileDtoHierarchy(FileDto fileDto) {
        List<FileDto> subFiles = fileDto.getSubFiles();
        if (!CollectionUtils.isEmpty(subFiles)) {
            List<FileEntity> subEntities = subFiles.stream().map(this::saveFileDtoHierarchy).toList();
            FileEntity entity = new FileEntity(fileDto);
            entity.setSubFiles(subEntities);
            return fileEntityRepo.save(entity);
        }
        FileEntity entity = new FileEntity(fileDto);
        return fileEntityRepo.save(entity);
    }
}
