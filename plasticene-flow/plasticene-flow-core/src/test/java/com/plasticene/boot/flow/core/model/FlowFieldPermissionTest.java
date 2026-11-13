package com.plasticene.boot.flow.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证节点字段权限的序列化及旧模型兼容。
 * @author ZFJ
 * @since 2026-11-04
 */
class FlowFieldPermissionTest {
    @Test
    void shouldRoundTripAndAcceptLegacyNodes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        FlowNode root = mapper.readValue("{\"key\":\"start\",\"childNode\":{\"fieldsPermission\":[{\"field\":\"reason\",\"title\":\"原因\",\"permission\":\"HIDDEN\"}]}}", FlowNode.class);
        FlowNode restored = mapper.readValue(mapper.writeValueAsString(root), FlowNode.class);
        assertEquals(FlowFieldPermission.Permission.HIDDEN, restored.getChildNode().getFieldsPermission().getFirst().getPermission());
        assertNull(restored.getFieldsPermission());
    }

    @Test
    void shouldRejectUnknownJsonPermissions() {
        ObjectMapper mapper = new ObjectMapper();
        for (String value : new String[]{"3", "\"NONE\"", "\"INVALID\""}) {
            assertThrows(JsonProcessingException.class,
                    () -> mapper.readValue("{\"field\":\"reason\",\"permission\":" + value + "}", FlowFieldPermission.class));
        }
    }
}
