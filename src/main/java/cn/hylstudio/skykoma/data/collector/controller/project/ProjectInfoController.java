package cn.hylstudio.skykoma.data.collector.controller.project;

import cn.hylstudio.skykoma.data.collector.controller.AbstractController;
import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.*;
import cn.hylstudio.skykoma.data.collector.model.payload.*;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static cn.hylstudio.skykoma.data.collector.entity.neo4j.ScanRecordEntity.ALLOW_STATUS;

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

    @RequestMapping(value = "/updateProjectBasicInfo", method = RequestMethod.POST)
//    public JsonResult<ProjectInfoDto> uploadProjectInfo(@RequestBody ProjectInfoUploadPayload payload) {
    public JsonResult<ProjectInfoDto> updateProjectBasicInfo(@RequestBody String payloadJson) {
        ProjectInfoUploadPayload payload = gson.fromJson(payloadJson, ProjectInfoUploadPayload.class);
        debugJson(payloadJson, payload.getScanId(), payload.getProjectInfoDto().getName());
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
            throw new BizException(BizCode.WRONG_PARAMS, "projectKey empty");
        }
        List<ModuleDto> moduleDtos = projectInfoDto.getModules();
        if (CollectionUtils.isEmpty(moduleDtos)) {
            throw new BizException(BizCode.WRONG_PARAMS, "modules empty");
        }
        VCSEntityDto vcsEntityDto = projectInfoDto.getVcsEntityDto();
        if (vcsEntityDto == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "vcsEntityDto empty");
        }
        LOGGER.info("uploadProjectInfo, projectKey = [{}], scanId = [{}]", projectKey, scanId);
        projectInfoService.updateProjectBasicInfoAsync(payload);
        return JsonResult.succ(null);
    }

    @RequestMapping(value = "/updateProjectFileInfo", method = RequestMethod.POST)
//    public JsonResult<ProjectInfoDto> updateProjectFileInfo(@RequestBody ProjectFileInfoUploadPayload payload) {
    public JsonResult<ProjectInfoDto> updateProjectFileInfo(@RequestBody String payloadJson) {
        ProjectFileInfoUploadPayload payload = gson.fromJson(payloadJson, ProjectFileInfoUploadPayload.class);
        debugJson(payloadJson, payload.getScanId(), payload.getFileDto().getName());
        String scanId = payload.getScanId();
        if (!StringUtils.hasText(scanId)) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId empty");
        }
        FileDto fileDto = payload.getFileDto();
        if (fileDto == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "projectInfoDto empty");
        }
        LOGGER.info("updateProjectFileInfo, scanId = [{}], fileName = [{}]", scanId, fileDto.getName());
        projectInfoService.updateProjectFileInfoAsync(payload);
        return JsonResult.succ(null);
    }

    @RequestMapping(value = "/queryScan", method = RequestMethod.POST)
    public JsonResult<ScanRecordDto> queryScan(@RequestBody QueryScanPayload payload) {
        String scanId = payload.getScanId();
        if (!StringUtils.hasText(scanId)) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId empty");
        }
        LOGGER.info("queryScan, payload = [{}]", payload);
        ScanRecordDto result = projectInfoService.queryScan(payload);
        return JsonResult.succ(result);
    }

    @RequestMapping(value = "/beginScan", method = RequestMethod.POST)
    public JsonResult<ScanRecordDto> beginScan(@RequestBody BeginScanPayload payload) {
        String scanId = payload.getScanId();
        if (!StringUtils.hasText(scanId)) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId empty");
        }
        ProjectInfoDto projectInfoDto = payload.getProjectInfoDto();
        if (projectInfoDto == null) {
            throw new BizException(BizCode.WRONG_PARAMS, "projectInfoDto empty");
        }
        LOGGER.info("updateScanStatus, payload = [{}]", payload);
        ScanRecordDto result = projectInfoService.beginScan(payload);
        return JsonResult.succ(result);
    }

    @RequestMapping(value = "/updateScanStatus", method = RequestMethod.POST)
    public JsonResult<ScanRecordDto> updateScanStatus(@RequestBody UpdateScanStatusPayload payload) {
        String scanId = payload.getScanId();
        if (!StringUtils.hasText(scanId)) {
            throw new BizException(BizCode.WRONG_PARAMS, "scanId empty");
        }
        String status = payload.getStatus();
        if (!StringUtils.hasText(status)) {
            throw new BizException(BizCode.WRONG_PARAMS, "status empty");
        }
        if (!ALLOW_STATUS.contains(status)) {
            throw new BizException(BizCode.WRONG_PARAMS, "status error");
        }
        LOGGER.info("updateScanStatus, payload = [{}]", payload);
        ScanRecordDto result = projectInfoService.updateScanStatus(payload);
        return JsonResult.succ(result);
    }

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${debug.file.path}")
    private String debugPath;
    private void debugJson(String payloadJson, String scanId, String fileName) {

        try {
            String rootPath = debugPath;
            // String rootPath = "";
            String parentPath = "%s/%s".formatted(rootPath, scanId);
            if (!new File(parentPath).exists()) {
                new File(parentPath).mkdirs();
            }
            String filePath = "%s/%s/%s.json".formatted(rootPath, scanId, fileName);
            File file = new File(filePath);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(payloadJson);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            LOGGER.info("debugJson error, e = [{}]", e.getMessage(), e);
        }
    }
}
