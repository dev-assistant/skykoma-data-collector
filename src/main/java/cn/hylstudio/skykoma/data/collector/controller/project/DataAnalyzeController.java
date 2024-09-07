package cn.hylstudio.skykoma.data.collector.controller.project;

import cn.hylstudio.skykoma.data.collector.controller.AbstractController;
import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.BizCode;
import cn.hylstudio.skykoma.data.collector.model.JsonResult;
import cn.hylstudio.skykoma.data.collector.model.payload.DataAnalyzeTriggerPayload;
import cn.hylstudio.skykoma.data.collector.service.IBizDataAnalyzeService;
import cn.hylstudio.skykoma.data.collector.service.IBizProjectInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data/analyze")
public class DataAnalyzeController extends AbstractController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataAnalyzeController.class);
    @Autowired
    private IBizProjectInfoService projectInfoService;
    @Autowired
    private IBizDataAnalyzeService bizDataAnalyzeService;

    @RequestMapping(value = "/trigger", method = RequestMethod.POST)
    public JsonResult<String> triggerAnalyze(@RequestBody DataAnalyzeTriggerPayload payload) {
        LOGGER.info("triggerAnalyze, payload = [{}]", payload);
        String scanId = payload.getScanId();
        if (!StringUtils.hasText(scanId)) {
            throw new BizException(BizCode.WRONG_PARAMS, "empty scanId");
        }
        bizDataAnalyzeService.triggerAnalyzeAsync(scanId);
        return JsonResult.succ(scanId);
    }

}
