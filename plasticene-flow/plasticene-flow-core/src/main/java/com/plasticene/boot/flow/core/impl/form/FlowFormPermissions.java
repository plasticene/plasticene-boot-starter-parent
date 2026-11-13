package com.plasticene.boot.flow.core.impl.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.model.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.model.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.model.FlowFieldPermission;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.impl.tree.FlowParser;
import com.plasticene.boot.flow.core.impl.tree.FlowReturnPath;

import java.util.*;
import static com.plasticene.boot.flow.core.model.FlowFieldPermission.Permission.*;

/**
 * 按发布快照、用户参与记录及具体审批任务计算表单权限并过滤详情数据。
 * @author ZFJ
 * @since 2026-11-04
 */
public final class FlowFormPermissions {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FlowFormPermissions() { }

    /** 任务上下文优先；普通查看合并本人已参与节点的可读范围。 */
    public static Map<String, FlowFieldPermission.Permission> resolve(
            FlowDefinition definition, FlowInstance instance, List<FlowTask> tasks, Long userId, Long taskId) {
        Map<String, String> fields = fields(definition.getFormConfig());
        Map<String, FlowFieldPermission.Permission> result = new LinkedHashMap<>();
        fields.keySet().forEach(field -> result.put(field, HIDDEN));
        if (taskId != null) {
            FlowTask task = tasks.stream().filter(item -> Objects.equals(item.getId(), taskId)
                    && Objects.equals(item.getAssignee(), userId) && !Objects.equals(item.getIsDelete(), 1))
                    .findFirst().orElseThrow(() -> new BizException("任务不存在或无权查看"));
            FlowNode node = FlowParser.findNodeByKey(definition.getModelNode(), task.getNodeKey());
            boolean editable = FlowReturnPath.isActive(task)
                    && Objects.equals(instance.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())
                    && Objects.equals(task.getStatus(), FlowTaskStatusEnum.RUNNING.getCode())
                    && Objects.equals(task.getNodeType(), FlowNodeEnum.Type.APPROVE.getCode());
            fields.keySet().forEach(field -> result.put(field, permission(node, field, editable)));
            return result;
        }
        List<FlowNode> nodes = new ArrayList<>();
        if (Objects.equals(instance.getUserId(), userId)) {
            nodes.add(definition.getModelNode());
        }
        tasks.stream().filter(task -> Objects.equals(task.getAssignee(), userId)
                        && !Objects.equals(task.getIsDelete(), 1))
                .map(task -> FlowParser.findNodeByKey(definition.getModelNode(), task.getNodeKey()))
                .filter(Objects::nonNull).forEach(nodes::add);
        fields.keySet().forEach(field -> {
            if (nodes.stream().anyMatch(node -> permission(node, field, false) != HIDDEN)) {
                result.put(field, READ);
            }
        });
        return result;
    }

    /** 只允许更新表单中的可编辑字段，保留业务变量及其他审批人的字段修改。 */
    public static Map<String, Object> mergeChanges(Map<String, Object> current, Map<String, Object> changes,
            Map<String, FlowFieldPermission.Permission> permissions) {
        if (changes == null) {
            throw new BizException("表单修改不能为空");
        }
        Map<String, Object> result = new LinkedHashMap<>(current == null ? Map.of() : current);
        changes.forEach((field, value) -> {
            if (permissions.get(field) != WRITE) {
                throw new BizException("无权修改表单字段：" + field);
            }
            result.put(field, value);
        });
        return result;
    }

    /** 详情只返回获准查看的表单字段，隐藏字段及业务内部变量不返回。 */
    public static Map<String, Object> filterData(Map<String, Object> data,
            Map<String, FlowFieldPermission.Permission> permissions) {
        Map<String, Object> result = new LinkedHashMap<>();
        permissions.forEach((field, value) -> {
            if (value != HIDDEN && data.containsKey(field)) {
                result.put(field, data.get(field));
            }
        });
        return result;
    }

    /** 移除隐藏规则及配置中的默认字段值；使用副本，避免修改发布快照。 */
    public static Map<String, Object> filterConfig(Map<String, Object> config,
            Map<String, FlowFieldPermission.Permission> permissions) {
        if (config == null) {
            return null;
        }
        Map<String, Object> copy = MAPPER.convertValue(config, new TypeReference<>() { });
        copy.put("rule", filterRules(rules(copy), permissions));
        if (copy.get("option") instanceof Map<?, ?> option) {
            option.remove("formData");
        }
        return copy;
    }

    private static FlowFieldPermission.Permission permission(FlowNode node, String field, boolean editable) {
        if (node == null) {
            return HIDDEN;
        }
        FlowFieldPermission.Permission value = Objects.equals(node.getType(), FlowNodeEnum.Type.START.getCode()) ? WRITE : READ;
        if (node.getFieldsPermission() != null) {
            value = node.getFieldsPermission().stream().filter(item -> field.equals(item.getField()))
                    .map(FlowFieldPermission::getPermission).findFirst().orElse(value);
        }
        if (value == WRITE && (!editable || !Objects.equals(node.getType(), FlowNodeEnum.Type.APPROVE.getCode()))) {
            return READ;
        }
        return value;
    }

    private static Map<String, String> fields(Map<String, Object> config) {
        Map<String, String> result = new LinkedHashMap<>();
        collect(rules(config), result);
        return result;
    }

    private static void collect(List<?> rules, Map<String, String> result) {
        for (Object raw : rules) {
            if (raw == null || raw instanceof String text && !text.trim().startsWith("{")) {
                continue;
            }
            Map<String, Object> rule = rule(raw);
            if (rule.get("field") instanceof String field && rule.get("title") != null) {
                result.put(field, String.valueOf(rule.get("title")));
            }
            if (rule.get("children") instanceof List<?> children) {
                collect(children, result);
            }
        }
    }

    private static List<?> rules(Map<String, Object> config) {
        return config != null && config.get("rule") instanceof List<?> list ? list : List.of();
    }

    private static Map<String, Object> rule(Object value) {
        try {
            return value instanceof String text ? MAPPER.readValue(text, new TypeReference<>() { })
                    : MAPPER.convertValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new BizException("流程表单配置无效");
        }
    }

    private static List<Object> filterRules(List<?> rules, Map<String, FlowFieldPermission.Permission> permissions) {
        List<Object> result = new ArrayList<>();
        for (Object raw : rules) {
            if (raw instanceof String text && !text.trim().startsWith("{")) {
                result.add(text);
                continue;
            }
            Map<String, Object> item = rule(raw);
            if (item == null) {
                continue;
            }
            if (item.get("field") instanceof String field) {
                if (permissions.getOrDefault(field, HIDDEN) == HIDDEN) {
                    continue;
                }
                item.remove("value");
            }
            if (item.get("children") instanceof List<?> children) {
                item.put("children", filterRules(children, permissions));
            }
            if (item.get("control") instanceof List<?> controls) {
                for (Object control : controls) {
                    if (control instanceof Map<?, ?> entry && entry.get("rule") instanceof List<?> nested) {
                        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) entry;
                        map.put("rule", filterRules(nested, permissions));
                    }
                }
            }
            result.add(item);
        }
        return result;
    }
}
