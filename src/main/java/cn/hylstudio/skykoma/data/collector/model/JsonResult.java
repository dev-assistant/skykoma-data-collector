package cn.hylstudio.skykoma.data.collector.model;

import cn.hylstudio.skykoma.data.collector.ex.BizException;
import lombok.Data;

@Data
public class JsonResult<T> {
    private String code;
    private String msg;
    private T data;

    private JsonResult() {

    }

    public JsonResult(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> JsonResult<T> error(BizException exception) {
        return new JsonResult<>(exception.getCode(), exception.getMsg(), null);
    }

    public static <T> JsonResult<T> error(BizCode code) {
        return new JsonResult<>(code.getCode(), code.getMsg(), null);
    }

    public static <T> JsonResult<T> succ(T data) {
        return new JsonResult<>(BizCode.SUCC.getCode(), BizCode.SUCC.getMsg(), data);
    }
}
