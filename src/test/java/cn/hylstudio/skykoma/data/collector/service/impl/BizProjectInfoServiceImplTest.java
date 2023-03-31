package cn.hylstudio.skykoma.data.collector.service.impl;

import cn.hylstudio.skykoma.data.collector.BootTests;
import cn.hylstudio.skykoma.data.collector.entity.neo4j.FileEntity;
import cn.hylstudio.skykoma.data.collector.model.ProjectInfoDto;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
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
    void testQueryProject() throws Exception {
        ProjectInfoQueryPayload payload = new ProjectInfoQueryPayload();
        payload.setName("test");
        payload.setKey("test");
        payload.setCreateIfNotExists(true);
        ProjectInfoDto projectInfoDto = bizProjectInfoService.queryProject(payload);
        LOGGER.info("testQueryProject, payload = [{}], projectInfoDto = [{}]", payload, projectInfoDto);
    }
    @Test
    void uploadProjectInfo() throws Exception {
        String fileContents = Files.readString(Paths.get("D:/1.json"));
        Gson gson = new Gson();
        ProjectInfoUploadPayload projectInfoUploadPayload = gson.fromJson(fileContents, ProjectInfoUploadPayload.class);
        bizProjectInfoService.updateProjectInfoAsync(projectInfoUploadPayload);
    }
    @Test
    public void testQueryFile(){
        String scanId = "335b801282f94e9cb4e7e8bd3952f65b";
        String relativePath = "src/main/java/com/iqiyi/hotchat/account/util/ImageHandleHelper.java";
        FileEntity fileEntity = fileEntityRepo.findByScanIdAndRelativePath(scanId, relativePath);
        LOGGER.info("testQueryFile, scanId = [{}], relativePath = [{}], fileEntity = [{}]", scanId, relativePath, fileEntity);
    }
}