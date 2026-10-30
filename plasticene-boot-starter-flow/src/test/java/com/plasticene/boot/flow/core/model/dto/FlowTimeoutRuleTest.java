package com.plasticene.boot.flow.core.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.exception.BizException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证超时规则继承、完整覆盖和规则校验。
 * @author ZFJ
 * @since 2026-10-22
 */
class FlowTimeoutRuleTest {
    @Test
    void shouldDefaultToDisabledAndPreserveOldDefinitions() throws Exception {
        FlowExtraSetting setting = new ObjectMapper().readValue("{}", FlowExtraSetting.class);
        assertNull(FlowTimeoutRule.resolve(new FlowNode(), setting));
        assertFalse(new FlowTimeoutRule().isEnabled());
    }

    @Test
    void shouldResolveInheritanceCustomAndDisabled() {
        FlowExtraSetting setting = new FlowExtraSetting();
        FlowTimeoutRule defaults = rule();
        setting.setTimeoutRule(defaults);
        FlowNode node = new FlowNode();
        assertSame(defaults, FlowTimeoutRule.resolve(node, setting));
        node.setTimeoutMode(FlowTimeoutRule.Mode.DISABLED);
        assertNull(FlowTimeoutRule.resolve(node, setting));
        node.setTimeoutMode(FlowTimeoutRule.Mode.CUSTOM);
        node.setTimeoutRule(new FlowTimeoutRule());
        assertSame(node.getTimeoutRule(), FlowTimeoutRule.resolve(node, setting));
        assertFalse(FlowTimeoutRule.resolve(node, setting).isEnabled());
    }

    @Test
    void shouldValidateActionAndOrderingAcrossUnits() {
        FlowTimeoutRule rule = rule();
        assertDoesNotThrow(rule::validate);
        rule.setAutoAfter(1);
        rule.setAutoUnit(FlowTimeoutRule.Unit.DAY);
        assertThrows(BizException.class, rule::validate);
        rule.setFirstReminder(23);
        assertDoesNotThrow(rule::validate);
        rule.setAction(null);
        assertThrows(BizException.class, rule::validate);
        rule.setAutoEnabled(false);
        assertDoesNotThrow(rule::validate);
        rule.setMaxReminders(0);
        assertThrows(BizException.class, rule::validate);
    }

    @Test
    void shouldSerializeWithoutDerivedProperties() throws Exception {
        String json = new ObjectMapper().writeValueAsString(rule());
        assertFalse(json.contains("\"enabled\""));
        assertEquals(FlowTimeoutRule.Action.APPROVE, new ObjectMapper().readValue(json, FlowTimeoutRule.class).getAction());
        assertEquals(1440, FlowTimeoutRule.minutes(1, FlowTimeoutRule.Unit.DAY));
        assertThrows(BizException.class, () -> FlowTimeoutRule.minutes(Integer.MAX_VALUE, FlowTimeoutRule.Unit.DAY));
    }

    private FlowTimeoutRule rule() {
        FlowTimeoutRule rule = new FlowTimeoutRule();
        rule.setReminderEnabled(true);
        rule.setFirstReminder(24);
        rule.setReminderInterval(8);
        rule.setMaxReminders(3);
        rule.setAutoEnabled(true);
        rule.setAutoAfter(48);
        rule.setAction(FlowTimeoutRule.Action.APPROVE);
        return rule;
    }
}
