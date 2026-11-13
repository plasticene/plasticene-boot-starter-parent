package com.plasticene.boot.flow.core.impl.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.exception.BizException;
import java.util.*;
import java.util.function.Consumer;

/**
 * 校验流程表单配置并提取可被流程节点引用的基础字段。
 * @author ZFJ
 * @since 2026-11-05
 */
public final class FlowFormConfig {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // 与前端 form-config.ts 的基础组件类型保持一致。
    private static final Set<String> BASIC_TYPES = Set.of(
            "input", "inputNumber", "radio", "checkbox", "select", "switch", "rate",
            "timePicker", "slider", "datePicker", "cascader", "upload", "aTransfer",
            "tree", "aTreeSelect", "fcEditor", "signaturePad");
    private static final Set<String> SUBFORM_TYPES = Set.of("group", "subForm", "tableForm", "tableFormColumn");

    private FlowFormConfig() { }

    /** 保存时仅校验表单，不涉及流程节点树。 */
    public static void validate(Map<String, Object> config) {
        Set<String> fields = new HashSet<>();
        walk(config == null ? null : config.get("rule"), false, rule -> {
            String type = String.valueOf(rule.get("type"));
            Object field = rule.get("field");
            if (field != null || BASIC_TYPES.contains(type) || SUBFORM_TYPES.contains(type) && !"tableFormColumn".equals(type)) {
                if (blank(field)) {
                    throw new BizException("表单字段标识不能为空");
                }
                if (!fields.add(String.valueOf(field))) {
                    throw new BizException("表单字段标识不能重复：" + field);
                }
                if (blank(rule.get("title"))) {
                    throw new BizException("表单字段名称不能为空：" + field);
                }
            }
            validateOptions(rule, type);
        });
    }

    /** 发布时只提取允许引用的字段，不重复校验表单配置。 */
    public static Set<String> referenceFields(Map<String, Object> config) {
        Set<String> fields = new HashSet<>();
        walk(config == null ? null : config.get("rule"), true, rule -> {
            if (BASIC_TYPES.contains(String.valueOf(rule.get("type"))) && !blank(rule.get("field"))) {
                fields.add(String.valueOf(rule.get("field")));
            }
        });
        return fields;
    }

    private static boolean blank(Object value) {
        return value == null || value instanceof String text && text.isBlank();
    }

    private static void validateOptions(Map<String, Object> rule, String type) {
        Map<?, ?> props = rule.get("props") instanceof Map<?, ?> map ? map : Map.of();
        Object options;
        String labelKey = "label";
        String valueKey = "value";
        switch (type) {
            case "select", "radio", "checkbox" -> options = rule.get("options");
            case "cascader" -> options = props.get("options");
            case "tree", "aTreeSelect" -> {
                options = props.get("treeData");
                valueKey = "tree".equals(type) ? "id" : "value";
            }
            case "aTransfer" -> {
                options = props.get("dataSource");
                valueKey = "key";
            }
            default -> { return; }
        }
        if (props.get("fieldNames") instanceof Map<?, ?> names) {
            Object label = names.get("label");
            if (label == null) {
                label = names.get("title");
            }
            if (label instanceof String text) {
                labelKey = text;
            }
            Object value = names.get("value");
            if (value == null) {
                value = names.get("key");
            }
            if (value instanceof String text) {
                valueKey = text;
            }
        }
        validateOptionList(options, labelKey, valueKey, String.valueOf(rule.get("title")));
    }

    private static void validateOptionList(Object options, String labelKey, String valueKey, String title) {
        if (!(options instanceof List<?> list) || list.isEmpty()) {
            throw new BizException("表单字段「" + title + "」必须配置选项");
        }
        for (Object option : list) {
            if (!(option instanceof Map<?, ?> map) || blank(map.get(labelKey)) || blank(map.get(valueKey))) {
                throw new BizException("表单字段「" + title + "」的选项名称和值不能为空");
            }
            if (map.get("children") instanceof List<?> children && !children.isEmpty()) {
                validateOptionList(children, labelKey, valueKey, title);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readRule(Object raw) {
        // 请求反序列化后的规则可直接读取，避免逐层转换时重复复制整棵表单树。
        if (raw instanceof Map<?, ?>) {
            return (Map<String, Object>) raw;
        }
        try {
            return raw instanceof String text ? MAPPER.readValue(text, new TypeReference<>() { })
                    : MAPPER.convertValue(raw, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new BizException("流程表单配置无效");
        }
    }

    private static void walk(Object rawRules, boolean referencesOnly, Consumer<Map<String, Object>> visitor) {
        if (rawRules == null) {
            return;
        }
        if (!(rawRules instanceof List<?> rules)) {
            throw new BizException("流程表单规则必须是数组");
        }
        for (Object raw : rules) {
            if (raw == null || raw instanceof String text && !text.trim().startsWith("{")) {
                continue;
            }
            Map<String, Object> rule = readRule(raw);
            if (referencesOnly && SUBFORM_TYPES.contains(String.valueOf(rule.get("type")))) {
                continue;
            }
            visitor.accept(rule);
            walk(rule.get("children"), referencesOnly, visitor);
            if (rule.get("control") instanceof List<?> controls) {
                for (Object control : controls) {
                    if (control instanceof Map<?, ?> map && map.get("rule") instanceof List<?> nested) {
                        walk(nested.stream().filter(item -> !(item instanceof String)).toList(), referencesOnly, visitor);
                    }
                }
            }
            if (!referencesOnly && SUBFORM_TYPES.contains(String.valueOf(rule.get("type")))
                    && rule.get("props") instanceof Map<?, ?> props) {
                walk(props.get("rule"), false, visitor);
                if (props.get("columns") instanceof List<?> columns) {
                    for (Object column : columns) {
                        if (column instanceof Map<?, ?> map) {
                            walk(map.get("rule"), false, visitor);
                        }
                    }
                }
            }
        }
    }
}
