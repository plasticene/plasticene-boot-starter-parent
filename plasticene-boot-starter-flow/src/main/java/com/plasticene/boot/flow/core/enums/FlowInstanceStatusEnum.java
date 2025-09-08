package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * @author ZFJ
 * @date 2025/9/8
 */
@Getter
public enum FlowInstanceStatusEnum {
    RUNNING(0, "审批中"),
    APPROVE(1, "审批通过"),
    REJECT(2, "审批拒绝")

    ;

    private final Integer code;
    private final String name;

    FlowInstanceStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
