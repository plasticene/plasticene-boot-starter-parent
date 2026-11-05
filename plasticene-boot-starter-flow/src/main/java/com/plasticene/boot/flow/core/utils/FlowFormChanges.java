package com.plasticene.boot.flow.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.model.dto.FlowFormFieldChange;
import com.plasticene.boot.flow.core.model.dto.FlowFieldPermission.Permission;
import java.math.BigDecimal;
import java.util.*;

/**
 * 比较业务字段并生成历史值及展示文本快照。
 * @author ZFJ
 * @since 2026-11-04
 */
public final class FlowFormChanges {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FlowFormChanges() { }

    public static List<FlowFormFieldChange> diff(Map<String, Object> config,
            Map<String, Object> before, Map<String, Object> after) {
        Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
        collect(config == null ? null : config.get("rule"), fields);
        List<FlowFormFieldChange> result = new ArrayList<>();
        fields.forEach((field, rule) -> {
            Object oldValue = before == null ? null : before.get(field);
            Object newValue = after.get(field);
            if (!Objects.equals(canonical(oldValue), canonical(newValue))) {
                FlowFormFieldChange change = new FlowFormFieldChange();
                change.setField(field);
                change.setTitle(String.valueOf(rule.get("title")));
                change.setOldValue(oldValue);
                change.setNewValue(newValue);
                change.setOldText(text(oldValue, rule.get("options")));
                change.setNewText(text(newValue, rule.get("options")));
                result.add(change);
            }
        });
        return result;
    }

    public static List<FlowFormFieldChange> visible(List<FlowFormFieldChange> fields,
            Map<String, Permission> permissions) {
        return fields.stream().filter(field -> {
            Permission permission = permissions.get(field.getField());
            return permission == Permission.READ || permission == Permission.WRITE;
        }).toList();
    }

    private static Object canonical(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        switch (value) {
            case Number number -> {
                return new BigDecimal(number.toString()).stripTrailingZeros();
            }
            case List<?> list -> {
                return list.isEmpty() ? null : list.stream().map(FlowFormChanges::canonical).toList();
            }
            case Map<?, ?> map -> {
                Map<String, Object> result = new TreeMap<>();
                map.forEach((key, item) -> result.put(String.valueOf(key), canonical(item)));
                return result;
            }
            default -> {
            }
        }
        return value;
    }

    private static String text(Object value, Object options) {
        if (canonical(value) == null) {
            return "空";
        }
        if (value instanceof List<?> list) {
            return String.join("、", list.stream().map(item -> text(item, options)).toList());
        }
        if (options instanceof List<?> list) {
            for (Object option : list) {
                if (option instanceof Map<?, ?> map
                        && Objects.equals(canonical(value), canonical(map.get("value")))
                        && map.get("label") != null) {
                    return String.valueOf(map.get("label"));
                }
            }
        }
        if (value instanceof Map<?, ?>) {
            try {
                return MAPPER.writeValueAsString(value);
            } catch (Exception exception) {
                throw new BizException("表单变更值无法序列化");
            }
        }
        return String.valueOf(value);
    }

    private static void collect(Object rawRules, Map<String, Map<String, Object>> fields) {
        if (!(rawRules instanceof List<?> rules)) {
            return;
        }
        for (Object raw : rules) {
            if (raw instanceof String text && !text.trim().startsWith("{")) {
                continue;
            }
            try {
                Map<String, Object> rule = raw instanceof String text
                        ? MAPPER.readValue(text, new TypeReference<>() { })
                        : MAPPER.convertValue(raw, new TypeReference<>() { });
                if (rule == null) {
                    continue;
                }
                if (rule.get("field") instanceof String field && rule.get("title") != null) {
                    fields.put(field, rule);
                }
                collect(rule.get("children"), fields);
                if (rule.get("control") instanceof List<?> controls) {
                    for (Object control : controls) {
                        if (control instanceof Map<?, ?> map) {
                            collect(map.get("rule"), fields);
                        }
                    }
                }
            } catch (Exception exception) {
                throw new BizException("流程表单配置无效");
            }
        }
    }
}
