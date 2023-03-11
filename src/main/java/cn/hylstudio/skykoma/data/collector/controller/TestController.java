package cn.hylstudio.skykoma.data.collector.controller;

import cn.hylstudio.skykoma.data.collector.entity.neo4j.*;
import cn.hylstudio.skykoma.data.collector.repo.neo4j.ProjectEntityRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    private ProjectEntityRepo projectEntityRepo;

    @RequestMapping("/query")
    public String test() {
//        ProjectEntity projectEntity = new ProjectEntity();
//        projectEntity.setName("test");
//        List<ModuleEntity> modules = new ArrayList<>();
//        ModuleEntity module = new ModuleEntity();
//        module.setName("main");
//        ArrayList<ModuleRootRel> roots = new ArrayList<>();
//        ModuleRootRel rel = new ModuleRootRel();
//        FileEntity srcRoot = new FileEntity();
//        srcRoot.setName("src");
//        srcRoot.setRelativePath("1/src");
//        srcRoot.setType("folder");
//        rel.setRoot(srcRoot);
//        rel.setType("src");
//        roots.add(rel);
//        module.setRoots(roots);
//        modules.add(module);
//        projectEntity.setModules(modules);
//        VCSEntity vcsEntity = new VCSEntity(vcsEntityDto);
//        projectEntity.setVcsEntity(vcsEntity);
//        vcsEntity.setName("test1");
//        vcsEntity.setVcsType("git");
//        vcsEntity.setPath("d:/code/1");
//        FileEntity folder = new FileEntity();
//        folder.setName("1");
//        folder.setType("folder");
//        folder.setRelativePath("");
//        HashSet<FileEntity> subFiles = new HashSet<>();
//        FileEntity subFile = new FileEntity();
//        subFile.setType("file");
//        subFile.setName("1.txt");
//        subFile.setRelativePath("1/1.txt");
//        subFiles.add(subFile);
//        folder.setSubFiles(subFiles);
//        vcsEntity.setRootFolder(folder);
//        ProjectEntity save = projectEntityRepo.save(projectEntity);
        return "test";
    }
}
