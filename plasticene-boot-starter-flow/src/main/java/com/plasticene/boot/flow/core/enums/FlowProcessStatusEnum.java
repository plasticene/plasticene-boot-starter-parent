package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * 流程模型状态
 * 一个流程 → 一个唯一标识code
 * 一个流程在{@link com.plasticene.boot.flow.core.entity.FlowProcess} 可能存在多条记录
 * 一个流程：只有能一条『草稿』记录、 一条『发布』记录，可以有多条『历史』记录
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
