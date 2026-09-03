package com.plasticene.boot.flow.core;

/**
 * 审批流领域异常。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class FlowException extends RuntimeException {

    private final String code;

    public FlowException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
