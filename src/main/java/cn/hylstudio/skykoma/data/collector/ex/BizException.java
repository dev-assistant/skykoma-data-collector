package cn.hylstudio.skykoma.data.collector.ex;

import cn.hylstudio.skykoma.data.collector.model.BizCode;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class BizException extends RuntimeException {
    private String code;
    private String msg;

    public BizException() {
    }

    public BizException(BizCode code) {
        this.code = code.getCode();
        this.msg = code.getMsg();
    }

    public BizException(BizCode code, String emptyProjectName) {
        this.code = code.getCode();
        this.msg = emptyProjectName;
    }
}
