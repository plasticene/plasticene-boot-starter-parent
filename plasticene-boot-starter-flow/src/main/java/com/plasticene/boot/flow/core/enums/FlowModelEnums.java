package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * @author ZFJ
 * @since 2026/4/13
 */
public class FlowModelEnums {

    @Getter
    public enum StartUserType {
        ALL(0, "全员"),
        USER(1, "指定人员"),
        DEPT(2, "指定部门"),
        ROLE(3, "指定角色")
        ;

        private final int code;
        private final String name;

        StartUserType(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

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
