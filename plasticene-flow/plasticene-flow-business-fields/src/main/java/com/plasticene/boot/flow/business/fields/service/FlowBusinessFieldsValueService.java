package com.plasticene.boot.flow.business.fields.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowInstanceFieldValueDAO;
import com.plasticene.boot.flow.business.fields.persistence.entity.FlowInstanceFieldValue;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsFieldResolver.Descriptor;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessFieldsLifecycle.InstanceData;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 校验、转换并同步审批实例的业务字段值。
 * @author ZFJ
 * @since 2026-11-20
 */
@RequiredArgsConstructor
public class FlowBusinessFieldsValueService {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private final FlowInstanceFieldValueDAO values;
    private final FlowBusinessFieldsFieldResolver resolver;

    public void sync(InstanceData instance) {
        if (instance.schema() == null || !Objects.equals(instance.schema().getListVersion(), 1)) {
            return;
        }
        Map<String, FlowInstanceFieldValue> existing = list(instance.orgId(), List.of(instance.instanceId())).stream()
                .collect(Collectors.toMap(FlowInstanceFieldValue::getFieldCode, value -> value));
        List<FlowInstanceFieldValue> inserts = new ArrayList<>();
        List<FlowInstanceFieldValue> updates = new ArrayList<>();
        List<Long> deletes = new ArrayList<>();
        for (var descriptor : resolver.published(instance.schema()).values()) {
            var previous = existing.remove(descriptor.field());
            Object raw = instance.variables().get(descriptor.sourceField());
            if (raw == null || "".equals(raw)) {
                if (previous != null) {
                    deletes.add(previous.getId());
                }
                continue;
            }
            var row = new FlowInstanceFieldValue();
            row.setOrgId(instance.orgId());
            row.setInstanceId(instance.instanceId());
            row.setFieldCode(descriptor.field());
            row.setValueKind(descriptor.kind().name());
            convert(row, raw, descriptor);
            if (previous == null) {
                row.setCreator(instance.actorId());
                row.setCreateTime(LocalDateTime.now());
            } else {
                if (sameValue(previous, row)) {
                    continue;
                }
                row.setId(previous.getId());
            }
            row.setUpdater(instance.actorId());
            row.setUpdateTime(LocalDateTime.now());
            if (previous == null) {
                inserts.add(row);
            } else {
                updates.add(row);
            }
        }
        existing.values().forEach(row -> deletes.add(row.getId()));
        if (!deletes.isEmpty()) {
            values.deleteByIds(deletes);
        }
        if (!inserts.isEmpty()) {
            values.insert(inserts);
        }
        if (!updates.isEmpty()) {
            values.updateById(updates);
        }
    }

    public List<FlowInstanceFieldValue> list(Long orgId, List<Long> instanceIds) {
        return instanceIds.isEmpty() ? List.of() : values.selectList(new LambdaQueryWrapper<FlowInstanceFieldValue>()
                .eq(FlowInstanceFieldValue::getOrgId, orgId).in(FlowInstanceFieldValue::getInstanceId, instanceIds));
    }

    public static BigDecimal number(String input) {
        if (!input.matches("[+-]?[0-9]+(?:[.][0-9]+)?")) {
            throw new IllegalArgumentException("数字格式无效");
        }
        BigDecimal result = new BigDecimal(input).stripTrailingZeros();
        if (result.scale() > 4 || result.precision() - result.scale() > 14) {
            throw new IllegalArgumentException("数字最多 14 位整数、4 位小数");
        }
        return result;
    }

    public static String normalizeValue(String value) {
        String normalized = FlowBusinessFieldsFieldResolver.normalize(value);
        if (value.codePointCount(0, value.length()) > 512 || normalized.codePointCount(0, normalized.length()) > 512) {
            throw new IllegalArgumentException("文本或选项最多 512 字符");
        }
        return normalized;
    }

    private void convert(FlowInstanceFieldValue row, Object raw, Descriptor field) {
        try {
            switch (field.kind()) {
                case NUMBER -> {
                    if (!(raw instanceof Number || raw instanceof String)) {
                        throw new IllegalArgumentException("需要数字");
                    }
                    String number = raw instanceof BigDecimal decimal ? decimal.toPlainString() : raw.toString();
                    if (raw instanceof Double || raw instanceof Float) {
                        number = new BigDecimal(number).toPlainString();
                    }
                    row.setNumberValue(number(number));
                }
                case DATETIME -> {
                    if (!(raw instanceof String date)) {
                        throw new IllegalArgumentException("需要日期时间字符串");
                    }
                    var parsed = LocalDateTime.parse(date, DATE_FORMAT);
                    if (parsed.getYear() < 1000 || parsed.getYear() > 9999) {
                        throw new IllegalArgumentException("日期年份超出范围");
                    }
                    row.setDateValue(parsed);
                }
                case TEXT, OPTION -> {
                    if (!(raw instanceof String) && (field.kind() == FlowBusinessFieldsFieldResolver.Kind.TEXT
                            || !(raw instanceof Number || raw instanceof Boolean))) {
                        throw new IllegalArgumentException("需要单值文本或选项编码");
                    }
                    String value = raw.toString();
                    String match = normalizeValue(value);
                    if (field.kind() == FlowBusinessFieldsFieldResolver.Kind.OPTION
                            && field.options().stream().noneMatch(option -> option.value().equals(match))) {
                        throw new IllegalArgumentException("选项不在发布定义中");
                    }
                    row.setTextValue(value);
                    row.setMatchValue(match);
                }
            }
        } catch (RuntimeException exception) {
            throw new BizException("审批列表字段 " + field.title() + "：" + exception.getMessage());
        }
    }

    private boolean sameValue(FlowInstanceFieldValue first, FlowInstanceFieldValue second) {
        return Objects.equals(first.getTextValue(), second.getTextValue())
                && Objects.equals(first.getMatchValue(), second.getMatchValue())
                && (first.getNumberValue() == null ? second.getNumberValue() == null
                        : second.getNumberValue() != null && first.getNumberValue().compareTo(second.getNumberValue()) == 0)
                && Objects.equals(first.getDateValue(), second.getDateValue());
    }
}
