package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Getter
public enum FlowConditionTypeEnum {
    AND(0, "AND"),
    OR(1, "OR"),
    ;

    private final Integer code;
    private final String name;

    FlowConditionTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
