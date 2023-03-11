package cn.hylstudio.skykoma.data.collector.model;

import lombok.Getter;

@Getter
public enum BizCode {
    SUCC("C000000", "succ"),
    WRONG_PARAMS("C000001", "wrong param"),
    NOT_FOUND("C000004", "wrong param");
    private String code;
    private String msg;

    private BizCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
