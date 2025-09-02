package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Getter
public enum FlowTaskStatusEnum {
    RUNNING(0, "处理中"),
    COMPLETE(1, "已完成"),
    REJECT(2, "拒绝");

    private final Integer code;
    private final String name;

    FlowTaskStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
