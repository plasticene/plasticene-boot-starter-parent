package com.plasticene.boot.flow.core.model.enums;

import com.plasticene.boot.common.exception.BizException;

/**
 * 定义自带表单与宿主业务表单类型。
 * @author ZFJ
 * @since 2026-11-16
 */
public enum FlowFormMode {
    FORM(0), BUSINESS(1);
    private final int code;
    FlowFormMode(int code) { this.code = code; }
    public int getCode() { return code; }
    public static FlowFormMode from(Integer code) {
        for (FlowFormMode mode : values()) {
            if (code != null && mode.code == code) { return mode; }
        }
        throw new BizException("表单类型必须为0（自带表单）或1（业务表单）");
    }
}
