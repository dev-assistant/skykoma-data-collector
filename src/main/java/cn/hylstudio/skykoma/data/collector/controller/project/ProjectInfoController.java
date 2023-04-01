package cn.hylstudio.skykoma.data.collector.controller.project;

import cn.hylstudio.skykoma.data.collector.controller.AbstractController;
import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.BizCode;
import cn.hylstudio.skykoma.data.collector.model.JsonResult;
import cn.hylstudio.skykoma.data.collector.model.ProjectInfoDto;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileWriter;

@RestController
@RequestMapping("/api/project")
public class ProjectInfoController extends AbstractController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectInfoController.class);

    @Autowired
    private IBizProjectInfoService projectInfoService;
    @Autowired
    private Gson gson;

    @RequestMapping(value = "/queryProjectInfo", method = RequestMethod.POST)
    public JsonResult<ProjectInfoDto> queryProjectInfo(@RequestBody ProjectInfoQueryPayload payload) {
        LOGGER.info("queryProjectInfo, payload = [{}]", payload.hashCode());
        Boolean createIfNotExists = payload.getCreateIfNotExists();
        if (createIfNotExists == null) {
            payload.setCreateIfNotExists(false);
        }
        ProjectInfoDto projectEntity = projectInfoService.queryProject(payload);
        return JsonResult.succ(projectEntity);
    }

    @RequestMapping(value = "/updateProjectInfo", method = RequestMethod.POST)
//    public JsonResult<ProjectInfoDto> uploadProjectInfo(@RequestBody ProjectInfoUploadPayload payload) {
    public JsonResult<ProjectInfoDto> uploadProjectInfo(@RequestBody String payloadJson) {
        debugJson(payloadJson);
        ProjectInfoUploadPayload payload = gson.fromJson(payloadJson, ProjectInfoUploadPayload.class);
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
        LOGGER.info("uploadProjectInfo, projectKey = [{}], scanId = [{}]", projectKey, scanId);
        projectInfoService.updateProjectInfoAsync(payload);
        return JsonResult.succ(null);
    }

    @Autowired
    private ResourceLoader resourceLoader;

    private void debugJson(String payloadJson) {
        try {
            Resource resource = resourceLoader.getResource("classpath:");
            File rootDir = resource.getFile();
            String rootPath = rootDir.getAbsolutePath();
            String fileName = "%s/%s.json".formatted(rootPath, System.currentTimeMillis());
            File file = new File(fileName);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(payloadJson);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {

        }
    }
}
