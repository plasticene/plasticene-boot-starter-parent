package com.plasticene.boot.flow.business.fields.service;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import com.plasticene.boot.flow.core.model.FlowFieldPermission;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 将发布变量定义转换为审批列表可用的字段描述。
 * @author ZFJ
 * @since 2026-11-20
 */
public class FlowBusinessFieldsFieldResolver {

    private static final Set<String> TEXT_COMPONENTS = Set.of("input");
    private static final Set<String> OPTION_COMPONENTS = Set.of("select", "radio");

    public enum Kind {
        TEXT, NUMBER, DATETIME, OPTION
    }

    public record Descriptor(String sourceField, String field, String title, Kind kind, Integer variableType,
                             List<FlowBusinessFieldsVO.Option> options) {

        public FlowBusinessFieldsVO.Field candidate() {
            return new FlowBusinessFieldsVO.Field(field, title, kind.name(), variableType, false, false, null, options);
        }
    }

    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    public FlowVariableSchema prepare(FlowVariableSchema schema, boolean business, FlowNode node) {
        Set<String> hiddenFields = business ? Set.of() : hiddenFields(node);
        var result = new FlowVariableSchema();
        var fields = new ArrayList<FlowVariableSchema.Field>();
        for (var source : schema.getFields()) {
            var field = copy(source);
            boolean declared = business ? Boolean.TRUE.equals(source.getAllowInList())
                    : !hiddenFields.contains(source.getField());
            field.setAllowInList(declared && kind(source) != null);
            if (business && declared && kind(source) == null) {
                throw invalid(source.getField(), "类型不支持用于列表");
            }
            fields.add(field);
        }
        result.setFields(List.copyOf(fields));
        descriptors(result);
        return result;
    }

    public Map<String, Descriptor> descriptors(FlowVariableSchema schema) {
        Map<String, Descriptor> result = new LinkedHashMap<>();
        if (schema == null) {
            return result;
        }
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (var field : schema.getFields()) {
            String key = normalize(field.getField());
            if (!seen.add(key)) {
                duplicates.add(key);
            }
            if (Boolean.TRUE.equals(field.getAllowInList())) {
                Kind kind = kind(field);
                if (key.equals("process_start_user_id") || key.codePointCount(0, key.length()) > 128 || kind == null) {
                    throw invalid(field.getField(), "标识或类型不支持用于列表");
                }
                var options = kind == Kind.OPTION ? options(field) : List.<FlowBusinessFieldsVO.Option>of();
                result.put(key, new Descriptor(field.getField(), key, field.getTitle(), kind, field.getType(), options));
            }
        }
        duplicates.retainAll(result.keySet());
        if (!duplicates.isEmpty()) {
            throw invalid(duplicates.iterator().next(), "标识存在大小写冲突");
        }
        return result;
    }

    public Map<String, Descriptor> published(FlowVariableSchema schema) {
        return schema != null && Objects.equals(schema.getListVersion(), 1) ? descriptors(schema) : Map.of();
    }

    private Kind kind(FlowVariableSchema.Field field) {
        Integer type = field.getType();
        String component = field.getComponentType();
        if (type == null || type < 0 || type > 2) {
            return null;
        }
        boolean hasOptions = field.getOptions() != null && !field.getOptions().isEmpty();
        if (hasOptions && (component == null || OPTION_COMPONENTS.contains(component))) {
            return Kind.OPTION;
        }
        return switch (type) {
            case 0 -> component == null || TEXT_COMPONENTS.contains(component) ? Kind.TEXT : null;
            case 1 -> Kind.NUMBER;
            case 2 -> Kind.DATETIME;
            default -> null;
        };
    }

    private List<FlowBusinessFieldsVO.Option> options(FlowVariableSchema.Field field) {
        Map<?, ?> names = field.getProps() != null && field.getProps().get("fieldNames") instanceof Map<?, ?> map
                ? map : Map.of();
        String valueKey = names.get("value") instanceof String value ? value : "value";
        String labelKey = names.get("label") instanceof String label ? label : "label";
        Set<String> codes = new HashSet<>();
        List<FlowBusinessFieldsVO.Option> options = new ArrayList<>();
        for (var item : field.getOptions()) {
            Object value = item == null ? null : item.get(valueKey);
            Object label = item == null ? null : item.get(labelKey);
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean)
                    || !(label instanceof String title) || title.isBlank()) {
                throw invalid(field.getField(), "选项编码或名称不合法");
            }
            String code = normalize(value.toString());
            if (code.isBlank() || code.codePointCount(0, code.length()) > 512 || !codes.add(code)) {
                throw invalid(field.getField(), "选项编码无效或不区分大小写时重复");
            }
            options.add(new FlowBusinessFieldsVO.Option(code, title, false));
        }
        return List.copyOf(options);
    }

    private Set<String> hiddenFields(FlowNode root) {
        Set<String> hidden = new HashSet<>();
        var queue = new ArrayDeque<FlowNode>();
        queue.add(root);
        while (!queue.isEmpty()) {
            FlowNode node = queue.removeFirst();
            var type = FlowNodeEnum.Type.getType(node.getType());
            if ((type == FlowNodeEnum.Type.START || type == FlowNodeEnum.Type.APPROVE || type == FlowNodeEnum.Type.COPY)
                    && node.getFieldsPermission() != null) {
                node.getFieldsPermission().stream()
                        .filter(field -> field.getPermission() == FlowFieldPermission.Permission.HIDDEN)
                        .map(FlowFieldPermission::getField)
                        .forEach(hidden::add);
            }
            if (node.getChildNode() != null) {
                queue.add(node.getChildNode());
            }
            if (node.getConditionNodes() != null) {
                queue.addAll(node.getConditionNodes());
            }
        }
        return hidden;
    }

    private FlowVariableSchema.Field copy(FlowVariableSchema.Field source) {
        var result = new FlowVariableSchema.Field();
        result.setField(source.getField());
        result.setTitle(source.getTitle());
        result.setType(source.getType());
        result.setRequired(source.getRequired());
        result.setElementType(source.getElementType());
        result.setFormat(source.getFormat());
        result.setComponentType(source.getComponentType());
        result.setProps(source.getProps());
        result.setOptions(source.getOptions());
        return result;
    }

    private BizException invalid(String field, String message) {
        return new BizException("审批列表字段 " + field + "：" + message);
    }
}
