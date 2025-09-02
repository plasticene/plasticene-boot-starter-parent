package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Getter
public enum FlowProcessStatusEnum {

    DRAFT(0, "草稿"),
    RELEASE(1, "已发布"),
    HISTORY(2, "历史");



    private final Integer code;
    private final String name;
    FlowProcessStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
