package cn.hylstudio.skykoma.data.collector.controller;

import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.BizCode;
import cn.hylstudio.skykoma.data.collector.model.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class AbstractController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractController.class);

    @ExceptionHandler(BizException.class)
    @ResponseBody
    public JsonResult<Object> handleBizException(BizException exception) {
        return JsonResult.error(exception);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public JsonResult<Object> handleException(Exception exception) {
        LOGGER.error("handle unknown error, e = [{}]", exception.getMessage(), exception);
        return new JsonResult<>(BizCode.SYSTEM_ERROR.getCode(),"unknown error", null);
    }

}
