package com.plasticene.boot.flow.business.fields.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.business.fields.persistence.entity.FlowInstanceFieldValue;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsFieldResolver.Kind;
import com.plasticene.boot.flow.core.api.param.FlowBusinessFieldFilter;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldValue;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO.Field;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowDefinitionDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsValueService.DATE_FORMAT;

/**
 * 编译可绑定的业务查询条件，并批量装配分页展示值。
 * @author ZFJ
 * @since 2026-11-20
 */
@RequiredArgsConstructor
public class FlowBusinessFieldsQuerySupport {

    private final FlowBusinessFieldsCategoryService categories;
    private final FlowBusinessFieldsValueService values;
    private final FlowBusinessFieldsFieldResolver resolver;
    private final FlowDefinitionDAO definitions;

    public enum Operator {
        EQ, STARTS_WITH, GT, GTE, LT, LTE, BETWEEN
    }

    public record Condition(String field, Kind kind, Operator operator, Object value, Object endValue) {
        public String getColumn() {
            return switch (kind) {
                case NUMBER -> "number_value";
                case DATETIME -> "date_value";
                case TEXT, OPTION -> "match_value";
            };
        }
    }

    public List<Condition> compile(Long orgId, String category, List<FlowBusinessFieldFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }
        if (category == null || category.isBlank()) {
            throw new BizException("请先选择具体分类再搜索业务字段");
        }
        var enabled = categories.searchable(orgId, category).stream().collect(Collectors.toMap(Field::field, f -> f));
        if (filters.size() > enabled.size()) {
            throw new BizException("业务字段条件超过分类允许数量");
        }
        Set<String> seen = new HashSet<>();
        return filters.stream().map(filter -> {
            if (filter == null || filter.field() == null) {
                throw new BizException("业务搜索字段不能为空");
            }
            String code = FlowBusinessFieldsFieldResolver.normalize(filter.field());
            Field field = enabled.get(code);
            if (field == null || !seen.add(code)) {
                throw new BizException("业务字段未启用搜索或条件重复：" + code);
            }
            try {
                return condition(field, filter);
            } catch (RuntimeException exception) {
                throw new BizException("业务搜索条件无效：" + field.title());
            }
        }).toList();
    }

    public <T> void enrich(Long orgId, String category, List<T> rows, Function<T, Long> instanceId,
                           Function<T, Long> definitionId, BiConsumer<T, Map<String, FlowBusinessFieldValue>> assign) {
        if (category == null || category.isBlank() || rows.isEmpty()) {
            return;
        }
        var display = categories.configured(orgId, category).stream().filter(Field::displayEnabled)
                .collect(Collectors.toMap(Field::field, f -> f));
        if (display.isEmpty()) {
            return;
        }
        var byInstance = values.list(orgId, rows.stream().map(instanceId).distinct().toList()).stream()
                .filter(value -> display.containsKey(value.getFieldCode()))
                .collect(Collectors.groupingBy(FlowInstanceFieldValue::getInstanceId));
        if (byInstance.isEmpty()) {
            return;
        }
        var snapshots = definitions.selectList(new LambdaQueryWrapper<FlowDefinition>()
                        .select(FlowDefinition::getId, FlowDefinition::getVarInfo)
                        .eq(FlowDefinition::getOrgId, orgId)
                        .in(FlowDefinition::getId, rows.stream().map(definitionId).distinct().toList()))
                .stream().collect(Collectors.toMap(FlowDefinition::getId, def -> resolver.published(def.getVarInfo())));
        for (T row : rows) {
            var snapshot = snapshots.getOrDefault(definitionId.apply(row), Map.of());
            Map<String, FlowBusinessFieldValue> result = new HashMap<>();
            for (var value : byInstance.getOrDefault(instanceId.apply(row), List.of())) {
                var descriptor = snapshot.get(value.getFieldCode());
                if (descriptor == null || !descriptor.kind().name().equals(value.getValueKind())
                        || !display.get(value.getFieldCode()).kind().equals(value.getValueKind())) {
                    continue;
                }
                String label = descriptor.options().stream().filter(option -> option.value().equals(value.getMatchValue()))
                        .map(option -> option.label()).findFirst().orElse(null);
                String text = switch (descriptor.kind()) {
                    case NUMBER -> value.getNumberValue().stripTrailingZeros().toPlainString();
                    case DATETIME -> DATE_FORMAT.format(value.getDateValue());
                    case TEXT, OPTION -> value.getTextValue();
                };
                result.put(value.getFieldCode(), new FlowBusinessFieldValue(text, label));
            }
            assign.accept(row, result);
        }
    }

    private Condition condition(Field field, FlowBusinessFieldFilter filter) {
        Kind kind = Kind.valueOf(field.kind());
        Operator operator = Operator.valueOf(filter.operator());
        Set<Operator> supported = switch (kind) {
            case TEXT -> Set.of(Operator.EQ, Operator.STARTS_WITH);
            case NUMBER -> Set.of(Operator.EQ, Operator.GT, Operator.GTE, Operator.LT, Operator.LTE, Operator.BETWEEN);
            case DATETIME -> Set.of(Operator.GTE, Operator.LTE, Operator.BETWEEN);
            case OPTION -> Set.of(Operator.EQ);
        };
        if (!supported.contains(operator) || filter.value() == null || filter.value().isEmpty()
                || operator != Operator.BETWEEN && filter.endValue() != null) {
            throw new IllegalArgumentException();
        }
        Object value = parse(kind, filter.value());
        Object end = operator == Operator.BETWEEN ? parse(kind, filter.endValue()) : null;
        if (end != null && (value instanceof BigDecimal number && number.compareTo((BigDecimal) end) > 0
                || value instanceof LocalDateTime date && date.isAfter((LocalDateTime) end))) {
            throw new IllegalArgumentException();
        }
        if (kind == Kind.OPTION && field.options().stream().noneMatch(option -> option.value().equals(
                FlowBusinessFieldsFieldResolver.normalize(filter.value())))) {
            throw new IllegalArgumentException();
        }
        if (operator == Operator.STARTS_WITH) {
            value = ((String) value).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        }
        return new Condition(field.field(), kind, operator, value, end);
    }

    private Object parse(Kind kind, String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return switch (kind) {
            case NUMBER -> FlowBusinessFieldsValueService.number(text);
            case DATETIME -> {
                var parsed = LocalDateTime.parse(text, DATE_FORMAT);
                if (parsed.getYear() < 1000 || parsed.getYear() > 9999) {
                    throw new IllegalArgumentException();
                }
                yield parsed;
            }
            case TEXT, OPTION -> FlowBusinessFieldsValueService.normalizeValue(text);
        };
    }
}
