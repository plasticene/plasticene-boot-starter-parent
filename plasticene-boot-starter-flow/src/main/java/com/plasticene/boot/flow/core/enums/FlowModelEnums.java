package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * @author ZFJ
 * @since 2026/4/13
 */
public class FlowModelEnums {

    @Getter
    public enum Status {
        DRAFT(0, "草稿"),
        PUBLISHED(1, "已发布"),
        DISABLED(-1, "已停用")
        ;

        private final int code;
        private final String name;

        Status(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
