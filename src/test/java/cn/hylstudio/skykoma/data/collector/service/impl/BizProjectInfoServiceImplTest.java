package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.BootTests;
import cn.hylstudio.skykoma.data.collector.model.ProjectDto;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;
import cn.hylstudio.skykoma.data.collector.repo.neo4j.FileEntityRepo;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Paths;

class BizProjectInfoServiceImplTest extends BootTests {
    @Autowired
    private IBizProjectInfoService bizProjectInfoService;
    @Autowired
    private FileEntityRepo fileEntityRepo;

    @Test
    void uploadProjectInfo() throws Exception {
        String fileContents = Files.readString(Paths.get("D:/1.json"));
        Gson gson = new Gson();
        ProjectInfoUploadPayload projectInfoUploadPayload = gson.fromJson(fileContents, ProjectInfoUploadPayload.class);
        ProjectDto projectDto = bizProjectInfoService.uploadProjectInfo(projectInfoUploadPayload);
        System.out.println(projectDto);
    }
}