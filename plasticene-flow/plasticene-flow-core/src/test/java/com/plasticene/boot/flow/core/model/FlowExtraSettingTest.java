package com.plasticene.boot.flow.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程更多设置测试
 *
 * @author ZFJ
 * @since 2026-09-28
 */
class FlowExtraSettingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeSettingsAsFlatJson() throws Exception {
        FlowExtraSetting setting = new FlowExtraSetting();
        setting.setAllowSubmitterCancel(false);
        setting.setAllowApproverWithdraw(true);
        setting.setApproverDeduplication(FlowExtraSetting.ApproverDeduplication.CONSECUTIVE);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(setting));

        assertEquals(3, json.size());
        assertFalse(json.get("allowSubmitterCancel").booleanValue());
        assertTrue(json.get("allowApproverWithdraw").booleanValue());
        assertEquals("CONSECUTIVE", json.get("approverDeduplication").textValue());
    }

    @Test
    void shouldSupplyDefaultsForExplicitNullFields() throws Exception {
        FlowExtraSetting setting = objectMapper.readValue(
                "{\"allowSubmitterCancel\":null,\"allowApproverWithdraw\":null,\"approverDeduplication\":null}",
                FlowExtraSetting.class);

        FlowExtraSetting normalized = FlowExtraSetting.normalize(setting);

        assertFalse(normalized.getAllowSubmitterCancel());
        assertFalse(normalized.getAllowApproverWithdraw());
        assertEquals(FlowExtraSetting.ApproverDeduplication.NONE, normalized.getApproverDeduplication());
    }

    @Test
    void shouldKeepCancellationEnabledWhenSettingsOrFieldAreAbsent() throws Exception {
        assertTrue(FlowExtraSetting.normalize(null).getAllowSubmitterCancel());
        FlowExtraSetting empty = objectMapper.readValue("{}", FlowExtraSetting.class);
        assertTrue(FlowExtraSetting.normalize(empty).getAllowSubmitterCancel());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPreserveExplicitCancellationSetting(boolean allowed) throws Exception {
        FlowExtraSetting setting = objectMapper.readValue(
                "{\"allowSubmitterCancel\":" + allowed + "}", FlowExtraSetting.class);

        assertEquals(allowed, FlowExtraSetting.normalize(setting).getAllowSubmitterCancel());
    }

}
