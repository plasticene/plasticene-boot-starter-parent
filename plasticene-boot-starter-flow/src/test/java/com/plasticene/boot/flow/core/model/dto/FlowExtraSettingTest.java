package com.plasticene.boot.flow.core.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
    void shouldSupplyCompatibleDefaults() {
        FlowExtraSetting setting = new FlowExtraSetting();
        setting.setAllowSubmitterCancel(null);
        setting.setAllowApproverWithdraw(null);
        setting.setApproverDeduplication(null);

        FlowExtraSetting normalized = FlowExtraSetting.normalize(setting);

        assertTrue(normalized.getAllowSubmitterCancel());
        assertFalse(normalized.getAllowApproverWithdraw());
        assertEquals(FlowExtraSetting.ApproverDeduplication.NONE, normalized.getApproverDeduplication());
    }
}
