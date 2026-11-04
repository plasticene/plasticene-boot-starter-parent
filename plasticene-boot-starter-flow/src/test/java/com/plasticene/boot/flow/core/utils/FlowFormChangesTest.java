package com.plasticene.boot.flow.core.utils;

import com.plasticene.boot.flow.core.model.dto.FlowFieldPermission.Permission;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证字段差异快照、展示文本及历史字段权限过滤。
 * @author ZFJ
 * @since 2026-11-04
 */
class FlowFormChangesTest {
    private final Map<String, Object> config = Map.of("rule", List.of(
            Map.of("field", "amount", "title", "金额"),
            Map.of("field", "status", "title", "状态", "options", List.of(
                    Map.of("value", 1, "label", "启用"), Map.of("value", 2, "label", "停用")))));

    @Test
    void snapshotsChangedFieldsAndOptionLabels() {
        var changes = FlowFormChanges.diff(config, Map.of("amount", 100, "status", 1),
                Map.of("amount", 1000, "status", 2, "internal", "ignored"));
        assertEquals(2, changes.size());
        assertEquals(100, changes.getFirst().getOldValue());
        assertEquals(1000, changes.getFirst().getNewValue());
        assertEquals("启用", changes.get(1).getOldText());
        assertEquals("停用", changes.get(1).getNewText());
        assertEquals("金额", changes.getFirst().getTitle());
    }

    @Test
    void ignoresEquivalentEmptyValuesObjectOrderAndNumericRepresentation() {
        Map<String, Object> before = new HashMap<>();
        before.put("amount", "");
        before.put("status", List.of());
        assertTrue(FlowFormChanges.diff(config, before, new HashMap<>()).isEmpty());
        assertTrue(FlowFormChanges.diff(config, Map.of("amount", 100), Map.of("amount", 100.0)).isEmpty());
        assertTrue(FlowFormChanges.diff(config, Map.of("amount", Map.of("a", 1, "b", 2)),
                Map.of("amount", Map.of("b", 2, "a", 1))).isEmpty());
        assertEquals(1, FlowFormChanges.diff(config, Map.of("amount", List.of(1, 2)),
                Map.of("amount", List.of(2, 1))).size());
    }

    @Test
    void clearingAndConsecutiveChangesUseCurrentDatabaseBaseline() {
        var first = FlowFormChanges.diff(config, Map.of("amount", 100), Map.of("amount", 1000));
        var second = FlowFormChanges.diff(config, Map.of("amount", first.getFirst().getNewValue()), Map.of());
        assertEquals(1000, second.getFirst().getOldValue());
        assertNull(second.getFirst().getNewValue());
        assertEquals("空", second.getFirst().getNewText());
    }

    @Test
    void visibilityDefaultsToHiddenAndDoesNotMutateSnapshot() {
        var changes = FlowFormChanges.diff(config, Map.of(), Map.of("amount", 100, "status", 1));
        assertEquals(1, FlowFormChanges.visible(changes, Map.of("amount", Permission.READ)).size());
        assertEquals(2, FlowFormChanges.visible(changes, Map.of("amount", Permission.WRITE, "status", Permission.READ)).size());
        assertTrue(FlowFormChanges.visible(changes, Map.of("amount", Permission.HIDDEN)).isEmpty());
        assertEquals(2, changes.size());
    }
}
