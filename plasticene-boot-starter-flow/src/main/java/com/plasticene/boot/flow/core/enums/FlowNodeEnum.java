package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

import java.util.stream.Stream;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public class FlowNodeEnum {

    @Getter
    public enum Type {
        // 0-10 操作节点
        END(-1, "结束节点"),
        START(0, "开始节点"),
        APPROVE(1, "审批节点"),
        COPY(2, "抄送节点"),

        // 10-20 条件节点及分支
        CONDITION_NODE(10, "条件结点"),
        CONDITION_BRANCH(11, "条件分支"),
        PARALLEL_BRANCH(12, "并行分支")
        ;

        private final Integer code;
        private final String name;

        Type(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
        public static Type getType(Integer code) {
            return Stream.of(values()).filter(type -> type.getCode().equals(code))
                    .findFirst()
                    .orElse(null);
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

    @Getter
    public enum ApproveMode {
        ALL(0, "会签"),
        ANY(1,"或签"),
        ORDER(2, "顺序审批")
        ;

        private final Integer code;
        private final String name;

        ApproveMode(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    public enum SelfApprove {
        MANUAL(0,"由提交人对自己审批"),
        AUTO_SKIP(1, "自动跳过"),
        TO_LEADER(2, "转给直属领导审批"),
        TO_DEPT_LEADER(3, "转给部门领导审批")
        ;

        private final Integer code;
        private final String name;

        SelfApprove(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    public enum AssigneeEmpty {
        AUTO_APPROVE(0, "自动通过"),
        AUTO_REJECT(1, "自动拒绝"),
        TO_USER(2,"指定人员审批"),
        TO_MANAGER(3, "转给管理员审批")
        ;

        private final Integer code;
        private final String name;

        AssigneeEmpty(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
    }




}

