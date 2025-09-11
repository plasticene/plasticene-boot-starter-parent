package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@Getter
public enum FieldTypeEnum {

    STRING(0, "字符串"),
    NUMBER(1, "数字"),
    DATE(2, "日期"),
    COLLECTION(3, "集合");


    private final Integer code;
    private final String name;

    FieldTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
