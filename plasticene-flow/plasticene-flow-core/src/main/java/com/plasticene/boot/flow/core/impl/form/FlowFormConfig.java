package com.plasticene.boot.flow.core.impl.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.exception.BizException;
import java.util.*;
import java.util.function.Consumer;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;

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
        validate(config, false);
    }

    /** 历史迁移按审批变量范围校验字段，排除子表单并保留历史选项。 */
    public static void validateMigrationSource(Map<String, Object> config) {
        validate(config, true);
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

    /** 将表单字段转为审批变量定义，内容字段保留但不扩充条件类型。 */
    public static FlowVariableSchema variableSchema(Map<String, Object> config) {
        var schema = new FlowVariableSchema();
        var fields = new ArrayList<FlowVariableSchema.Field>();
        walk(config == null ? null : config.get("rule"), true, rule -> {
            if (blank(rule.get("field"))) { return; }
            var field = new FlowVariableSchema.Field();
            String component = String.valueOf(rule.get("type"));
            Map<?, ?> props = rule.get("props") instanceof Map<?, ?> value ? value : Map.of();
            field.setField(String.valueOf(rule.get("field")));
            field.setTitle(String.valueOf(rule.getOrDefault("title", field.getField())));
            field.setComponentType(component);
            field.setRequired(Boolean.TRUE.equals(rule.get("$required")));
            field.setType(switch (component) {
                case "input" -> "number".equals(props.get("type")) ? 1 : 0;
                case "fcEditor", "timePicker", "switch", "signaturePad" -> 0;
                case "inputNumber", "rate", "slider" -> 1;
                case "datePicker" -> Boolean.TRUE.equals(props.get("range")) ? 3 : 2;
                case "checkbox", "cascader", "aTransfer", "tree" -> 3;
                case "select", "aTreeSelect" -> Boolean.TRUE.equals(props.get("multiple"))
                        || "multiple".equals(props.get("mode")) || "tags".equals(props.get("mode")) ? 3 : 0;
                case "radio" -> 0;
                default -> null;
            });
            if (Objects.equals(field.getType(), 2)) { field.setFormat("yyyy-MM-dd HH:mm:ss"); }
            field.setProps(MAPPER.convertValue(props, new TypeReference<Map<String, Object>>() { }));
            if (rule.get("options") instanceof List<?> options) {
                field.setOptions(MAPPER.convertValue(options, new TypeReference<List<Map<String, Object>>>() { }));
            }
            fields.add(field);
        });
        schema.setFields(fields);
        return schema;
    }

    private static void validate(Map<String, Object> config, boolean variableSourceOnly) {
        Set<String> fields = new HashSet<>();
        walk(config == null ? null : config.get("rule"), variableSourceOnly, rule -> {
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
            if (!variableSourceOnly) {
                validateOptions(rule, type);
            }
        });
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
