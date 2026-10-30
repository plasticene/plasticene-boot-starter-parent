package com.plasticene.boot.flow.core.enums;

import lombok.Getter;

/**
 * 维护超时通知的事件类型和发送状态编码。
 * @author ZFJ
 * @since 2026-10-30
 */
public final class FlowTimeoutNoticeEnums {
    private FlowTimeoutNoticeEnums() {
    }

    /** 通知事件类型，编码与数据库及通知接口保持一致。 */
    @Getter
    public enum EventType {
        REMINDER("REMINDER", "审批超时提醒"),
        RESULT("RESULT", "超时自动处理结果通知");

        private final String code;
        private final String name;

        EventType(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    /** 通知发送状态，FAILED 表示投递重试耗尽，CANCELLED 表示提醒失效。 */
    @Getter
    public enum State {
        PENDING("PENDING", "待发送"),
        SENT("SENT", "已发送"),
        FAILED("FAILED", "重试耗尽"),
        CANCELLED("CANCELLED", "提醒已失效");

        private final String code;
        private final String name;

        State(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
