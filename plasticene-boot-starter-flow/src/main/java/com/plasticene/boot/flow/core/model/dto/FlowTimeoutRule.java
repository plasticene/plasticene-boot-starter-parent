package com.plasticene.boot.flow.core.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.plasticene.boot.common.exception.BizException;
import lombok.Data;

/**
 * 定义待办超时提醒和自动处理规则。
 * @author ZFJ
 * @since 2026-10-22
 */
@Data
public class FlowTimeoutRule {
    /**
     * 是否开启超时提醒，默认关闭；与自动处理开关相互独立。
     */
    private Boolean reminderEnabled = false;
    /**
     * 从待办创建到首次提醒的时长，必须为正整数。
     */
    private Integer firstReminder;
    /**
     * 首次提醒时长单位，默认为小时。
     */
    private Unit firstReminderUnit = Unit.HOUR;
    /**
     * 相邻业务提醒轮次的时间间隔，必须为正整数。
     */
    private Integer reminderInterval;
    /**
     * 提醒间隔单位，默认为小时。
     */
    private Unit reminderIntervalUnit = Unit.HOUR;
    /**
     * 最多生成的业务提醒轮次，必须为正整数；发送重试不另算一轮。
     */
    private Integer maxReminders;
    /**
     * 是否开启超时自动处理，默认关闭。
     */
    private Boolean autoEnabled = false;
    /**
     * 从待办创建到自动处理的时长，必须为正整数，不受提醒和发送失败影响。
     */
    private Integer autoAfter;
    /**
     * 自动处理时长单位，默认为小时。
     */
    private Unit autoUnit = Unit.HOUR;
    /**
     * 超时自动动作，开启自动处理时必须显式选择，不设置默认值。
     */
    private Action action;

    /** 自然时间单位，包含夜间、周末和节假日。 */
    public enum Unit {
        /** 分钟。 */
        MINUTE,
        /** 小时，等于 60 分钟。 */
        HOUR,
        /** 天，固定等于 24 小时。 */
        DAY
    }
    /** 超时自动处理动作。 */
    public enum Action {
        /** 自动同意当前待办，沿用节点的多人审批方式流转。 */
        APPROVE,
        /** 自动拒绝当前待办，并终止整个流程。 */
        REJECT
    }
    /** 审批节点的超时规则来源。 */
    public enum Mode {
        /** 继承发布版本的流程默认规则，节点未设置模式时也采用此行为。 */
        INHERIT,
        /** 使用节点自定义规则，整套替换流程默认规则。 */
        CUSTOM,
        /** 关闭该节点的提醒和自动处理。 */
        DISABLED
    }

    @JsonIgnore
    public boolean isEnabled() {
        return Boolean.TRUE.equals(reminderEnabled) || Boolean.TRUE.equals(autoEnabled);
    }

    public static long minutes(Integer value, Unit unit) {
        if (value == null || value <= 0 || unit == null) {
            throw new BizException("超时时间必须为正整数且必须选择单位");
        }
        long minutes = value.longValue() * switch (unit) {
            case MINUTE -> 1;
            case HOUR -> 60;
            case DAY -> 1440;
        };
        if (minutes > 52_560_000) {
            throw new BizException("单项超时时间不能超过 100 年");
        }
        return minutes;
    }

    public void validate() {
        if (Boolean.TRUE.equals(reminderEnabled)) {
            minutes(firstReminder, firstReminderUnit);
            minutes(reminderInterval, reminderIntervalUnit);
            if (maxReminders == null || maxReminders <= 0) {
                throw new BizException("最多提醒轮次必须为正整数");
            }
        }
        if (Boolean.TRUE.equals(autoEnabled)) {
            long deadline = minutes(autoAfter, autoUnit);
            if (action == null) {
                throw new BizException("请选择超时自动同意或自动拒绝");
            }
            if (Boolean.TRUE.equals(reminderEnabled) && minutes(firstReminder, firstReminderUnit) >= deadline) {
                throw new BizException("首次提醒时间必须早于自动处理时间");
            }
        }
    }

    public static FlowTimeoutRule resolve(FlowNode node, FlowExtraSetting setting) {
        Mode mode = node.getTimeoutMode();
        if (mode == Mode.DISABLED) {
            return null;
        }
        return mode == Mode.CUSTOM ? node.getTimeoutRule() : setting == null ? null : setting.getTimeoutRule();
    }
}
