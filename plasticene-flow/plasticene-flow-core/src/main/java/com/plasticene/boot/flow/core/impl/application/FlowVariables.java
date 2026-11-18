package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessVariableProvider;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import com.plasticene.boot.flow.core.model.enums.FlowFormMode;
import com.plasticene.boot.flow.core.impl.form.FlowFormConfig;
import com.plasticene.boot.flow.core.impl.condition.Operator;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * 解析发布变量契约并校验实例输入，不读取运行时宿主业务数据。
 * @author ZFJ
 * @since 2026-11-16
 */
@Component
public class FlowVariables {
    @Resource
    private ObjectProvider<FlowBusinessVariableProvider> providers;

    /** 获取发布变量定义；业务表单须确认草稿定义与宿主最新定义一致。 */
    public FlowVariableSchema forModel(FlowModel model) {
        FlowFormMode mode = FlowFormMode.from(model.getFormMode());
        FlowVariableSchema schema;
        if (mode == FlowFormMode.FORM) {
            FlowFormConfig.validate(model.getFormConfig());
            schema = FlowFormConfig.variableSchema(model.getFormConfig());
        } else {
            FlowVariableSchema latest = forBusiness(model.getOrgId(), model.getCode());
            schema = model.getVarInfo() == null ? new FlowVariableSchema() : model.getVarInfo();
            validateSchema(schema, true);
            // 按字段标识比较完整定义，字段排列顺序不影响发布。
            var draftFields = schema.getFields().stream().collect(Collectors.toMap(
                    FlowVariableSchema.Field::getField, field -> field));
            var latestFields = latest.getFields().stream().collect(Collectors.toMap(
                    FlowVariableSchema.Field::getField, field -> field));
            if (!draftFields.equals(latestFields)) {
                throw new BizException("业务变量已调整，请刷新变量并适配工作流配置后重新发布");
            }

        }
        validateSchema(schema, mode == FlowFormMode.BUSINESS);
        return schema;
    }

    /** 根据可信组织与流程编码读取宿主契约，不读取或修改模型的表单类型。 */
    public FlowVariableSchema forBusiness(Long orgId, String processCode) {
        FlowBusinessVariableProvider provider = providers.getIfAvailable();
        FlowVariableSchema schema = provider == null ? null : provider.getSchema(orgId, processCode);
        if (schema == null) {
            schema = new FlowVariableSchema();
        }
        validateSchema(schema, true);
        return schema;
    }

    public void validateSchema(FlowVariableSchema schema, boolean business) {
        if (schema == null || schema.getFields() == null) { throw new BizException("变量定义不能为空"); }
        Set<String> names = new HashSet<>();
        for (var field : schema.getFields()) {
            if (field == null || field.getField() == null || field.getField().isBlank()
                    || field.getTitle() == null || field.getTitle().isBlank() || !names.add(field.getField())) {
                throw new BizException("变量字段标识、名称不能为空且标识不能重复");
            }
            if ("PROCESS_START_USER_ID".equals(field.getField())) { throw new BizException("不能声明系统流程变量"); }
            if (field.getElementType() != null && (field.getElementType() < 0 || field.getElementType() > 2)) { throw new BizException("集合元素类型不支持"); }
            if ((business && field.getType() == null) || (field.getType() != null && (field.getType() < 0 || field.getType() > 3))) {
                throw new BizException("变量类型不支持：" + field.getField());
            }
        }
    }

    public void validateData(FlowVariableSchema schema, Map<String, Object> input, boolean partial, boolean business) {
        validateData(schema, input, partial, business, true);
    }

    /** 路线预览允许必填变量尚未填写，仍校验已填写变量的类型及系统变量边界。 */
    public void validatePreviewData(FlowVariableSchema schema, Map<String, Object> input, boolean business) {
        validateData(schema, input, true, business, false);
    }

    private void validateData(FlowVariableSchema schema, Map<String, Object> input,
                              boolean partial, boolean business, boolean requireValues) {
        validateSchema(schema, business);
        Map<String, Object> data = input == null ? Map.of() : input;
        if (data.containsKey("PROCESS_START_USER_ID")) {
            throw new BizException("不能覆盖系统流程变量");
        }
        Set<String> names = new HashSet<>();
        for (var field : schema.getFields()) {
            names.add(field.getField());
            if (partial && !data.containsKey(field.getField())) {
                continue;
            }
            Object value = data.get(field.getField());
            if (value == null) {
                if (requireValues && Boolean.TRUE.equals(field.getRequired())) {
                    throw new BizException("变量不能为空：" + field.getTitle());
                }
                continue;
            }
            Integer type = field.getType();
            boolean valid = type == null || switch (type) {
                case 0 -> value instanceof String || !business && (value instanceof Number || value instanceof Boolean);
                case 1 -> value instanceof Number || !business && value instanceof String text
                        && field.getProps() != null && (Boolean.TRUE.equals(field.getProps().get("stringMode"))
                        || "number".equals(field.getProps().get("type"))) && validNumber(text);
                case 2 -> validDate(value);
                case 3 -> validCollection(value, field.getElementType());
                default -> false;
            };
            if (!valid) {
                throw new BizException("变量格式不正确：" + field.getTitle());
            }
        }
        if (business && !names.containsAll(data.keySet())) {
            throw new BizException("包含未声明的业务变量");
        }
    }

    private boolean validNumber(String value) {
        try { new BigDecimal(value); return true; }
        catch (NumberFormatException ignored) { return false; }
    }

    private boolean validDate(Object value) {
        if (!(value instanceof String text)) { return false; }
        try { LocalDateTime.parse(text, Operator.DATE_TIME_FORMATTER); return true; }
        catch (DateTimeParseException ignored) { return false; }
    }

    private boolean validCollection(Object value, Integer elementType) {
        Collection<?> values;
        if (value instanceof Collection<?> collection) { values = collection; }
        else if (value != null && value.getClass().isArray()) {
            var array = new ArrayList<Object>();
            for (int index = 0; index < Array.getLength(value); index++) {
                array.add(Array.get(value, index));
            }
            values = array;
        }
        else { return false; }
        if (elementType == null) { return true; }
        return values.stream().allMatch(item -> switch (elementType) {
            case 0 -> item instanceof String;
            case 1 -> item instanceof Number;
            case 2 -> validDate(item);
            default -> false;
        });
    }
}
