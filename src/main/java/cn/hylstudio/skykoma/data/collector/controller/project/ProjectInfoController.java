package cn.hylstudio.skykoma.data.collector.controller.project;

import cn.hylstudio.skykoma.data.collector.controller.AbstractController;
import cn.hylstudio.skykoma.data.collector.model.JsonResult;
import cn.hylstudio.skykoma.data.collector.model.ProjectDto;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoQueryPayload;
import cn.hylstudio.skykoma.data.collector.model.payload.ProjectInfoUploadPayload;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project")
public class ProjectInfoController extends AbstractController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectInfoController.class);

    @Autowired
    private IBizProjectInfoService projectInfoService;

    @RequestMapping(value = "/queryProjectInfo", method = RequestMethod.POST)
    public JsonResult<ProjectDto> queryProjectInfo(@RequestBody ProjectInfoQueryPayload payload) {
        LOGGER.info("queryProjectInfo, payload = [{}]", payload);
        Boolean createIfNotExists = payload.getCreateIfNotExists();
        if (createIfNotExists == null) {
            payload.setCreateIfNotExists(false);
        }
        ProjectDto projectEntity = projectInfoService.queryProject(payload);
        return JsonResult.succ(projectEntity);
    }

    @RequestMapping(value = "/uploadProjectInfo", method = RequestMethod.POST)
    public JsonResult<ProjectDto> uploadProjectInfo(@RequestBody ProjectInfoUploadPayload payload) {
        LOGGER.info("uploadProjectInfo, payload = [{}]", payload);
        ProjectDto projectEntity = projectInfoService.uploadProjectInfo(payload);
        return JsonResult.succ(projectEntity);
    }
}
