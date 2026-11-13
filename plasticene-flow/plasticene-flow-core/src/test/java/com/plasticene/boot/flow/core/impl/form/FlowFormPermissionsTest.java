package com.plasticene.boot.flow.core.impl.form;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.model.FlowFieldPermission;
import com.plasticene.boot.flow.core.model.FlowNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.plasticene.boot.flow.core.model.FlowFieldPermission.Permission.*;

/**
 * 验证详情字段权限继承、隐藏过滤及保存字段边界。
 * @author ZFJ
 * @since 2026-11-04
 */
class FlowFormPermissionsTest {
    @Test
    void shouldInheritTaskNodeForTransferAndAddSignAndDowngradeAfterHandling() {
        FlowDefinition definition = definition();
        FlowInstance instance = instance();
        FlowTask task = task();
        task.setSourceTaskId(40L);
        task.setAddSignId(50L);
        var permissions = FlowFormPermissions.resolve(definition, instance, List.of(task), 8L, 41L);
        assertEquals(WRITE, permissions.get("reason"));
        assertEquals(HIDDEN, permissions.get("secret"));
        for (int status : new int[]{1, 2, 3, 4, 5, 6}) {
            task.setStatus(status);
            assertEquals(READ, FlowFormPermissions.resolve(definition, instance, List.of(task), 8L, 41L).get("reason"));
        }
        task.setStatus(0);
        task.setReturnTaskId(99L);
        assertEquals(READ, FlowFormPermissions.resolve(definition, instance, List.of(task), 8L, 41L).get("reason"));
        task.setReturnTaskId(null);
        instance.setStatus(1);
        assertEquals(READ, FlowFormPermissions.resolve(definition, instance, List.of(task), 8L, 41L).get("reason"));
        assertThrows(BizException.class, () -> FlowFormPermissions.resolve(definition, instance, List.of(task), 9L, 41L));
    }

    @Test
    void shouldUseReadonlyUnionForViewingAndNeverGrantEditToCopy() {
        var definition = definition();
        var instance = instance();
        var task = task();
        var copy = new FlowNode();
        copy.setKey("copy"); copy.setType(2);
        definition.getModelNode().setChildNode(copy);
        FlowTask copyTask = task(); copyTask.setNodeKey("copy"); copyTask.setId(42L); copyTask.setNodeType(2);
        var ordinary = FlowFormPermissions.resolve(definition, instance, List.of(task, copyTask), 8L, null);
        assertEquals(READ, ordinary.get("reason"));
        assertEquals(READ, ordinary.get("secret"));
        assertEquals(READ, FlowFormPermissions.resolve(definition, instance, List.of(copyTask), 8L, 42L).get("reason"));
        assertEquals(HIDDEN, FlowFormPermissions.resolve(definition, instance, List.of(), 9L, null).get("reason"));
        assertEquals(HIDDEN, FlowFormPermissions.resolve(definition, instance, List.of(task), 8L, 41L).get("secret"));
    }

    @Test
    void shouldFilterHiddenDefaultsAndDataWithoutMutatingDefinition() {
        var definition = definition();
        var permissions = FlowFormPermissions.resolve(definition, instance(), List.of(task()), 8L, 41L);
        Map<String, Object> data = Map.of("reason", "old", "secret", "private", "internal", "system");
        assertEquals(Map.of("reason", "old"), FlowFormPermissions.filterData(data, permissions));
        var config = FlowFormPermissions.filterConfig(definition.getFormConfig(), permissions);
        String json = config.toString();
        assertFalse(json.contains("private"));
        assertFalse(json.contains("secret"));
        assertTrue(definition.getFormConfig().toString().contains("private"));
    }

    @Test
    void shouldMergeOnlyWritableFieldsAndPreserveOtherVariables() {
        Map<String, Object> current = Map.of("reason", "old", "secret", "private", "internal", 1);
        var permissions = FlowFormPermissions.resolve(definition(), instance(), List.of(task()), 8L, 41L);
        var updated = FlowFormPermissions.mergeChanges(current, Map.of("reason", "new"), permissions);
        assertEquals("new", updated.get("reason"));
        assertEquals("private", updated.get("secret"));
        assertEquals(1, updated.get("internal"));
        assertEquals("old", current.get("reason"));
        for (String field : List.of("secret", "internal", "unknown")) {
            assertThrows(BizException.class, () -> FlowFormPermissions.mergeChanges(current, Map.of(field, "bad"), permissions));
        }
        assertThrows(BizException.class, () -> FlowFormPermissions.mergeChanges(current, Map.of("reason", "bad"), Map.of("reason", READ)));
    }

    private FlowDefinition definition() {
        FlowDefinition definition = new FlowDefinition();
        FlowNode node = new FlowNode(); node.setKey("approve"); node.setType(1);
        FlowFieldPermission reason = new FlowFieldPermission(); reason.setField("reason"); reason.setPermission(WRITE);
        FlowFieldPermission secret = new FlowFieldPermission(); secret.setField("secret"); secret.setPermission(HIDDEN);
        node.setFieldsPermission(List.of(reason, secret));
        definition.setModelNode(node);
        definition.setFormConfig(Map.of("rule", List.of(Map.of("field", "reason", "title", "原因"), Map.of("field", "secret", "title", "秘密", "value", "private")), "option", Map.of("formData", Map.of("secret", "private"))));
        return definition;
    }

    private FlowInstance instance() {
        FlowInstance instance = new FlowInstance(); instance.setUserId(10L); instance.setStatus(0); return instance;
    }

    private FlowTask task() {
        FlowTask task = new FlowTask(); task.setId(41L); task.setAssignee(8L); task.setNodeKey("approve"); task.setNodeType(1); task.setStatus(0); return task;
    }
}
