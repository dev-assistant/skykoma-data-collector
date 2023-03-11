package cn.hylstudio.skykoma.data.collector.controller;

import cn.hylstudio.skykoma.data.collector.ex.BizException;
import cn.hylstudio.skykoma.data.collector.model.JsonResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AbstractController {
    @ExceptionHandler(Exception.class)
    public JsonResult<Object> handleException(BizException exception) {
        return JsonResult.error(exception);
    }

}
