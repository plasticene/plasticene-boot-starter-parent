package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public class FlowProcessNodeEnum {

    @Getter
    public enum Type {
        END(-1, "结束节点"),
        START(0, "开始节点"),
        APPROVE(1, "审批节点"),
        COPY(2, "抄送节点"),
        CONDITION_NODE(3, "条件结点"),
        CONDITION_BRANCH(4, "条件分支")
        ;

        private final Integer code;
        private final String name;

        Type(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    public enum ApproveType {
        MANUAL(0, "人工审批"),
        AUTO_PASS(1, "自动通过"),
        AUTO_REJECT(2, "自动拒绝")
        ;


        private final Integer code;
        private final String name;

        ApproveType(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    public enum AssigneeType {
        USER(0, "指定人员")


        ;
        private final Integer code;
        private final String name;

        AssigneeType(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
    }




}

