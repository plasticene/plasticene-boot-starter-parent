package com.plasticene.boot.flow.business.fields.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.business.fields.config.FlowBusinessFieldsProperties;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowCategoryFieldDAO;
import com.plasticene.boot.flow.business.fields.persistence.entity.FlowCategoryField;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsFieldResolver.Descriptor;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessFieldsLifecycle.Publication;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO.Field;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO.Option;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowCategoryDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowDefinitionDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowCategory;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理分类配置、发布兼容性和审批目录候选。
 * @author ZFJ
 * @since 2026-11-20
 */
@RequiredArgsConstructor
public class FlowBusinessFieldsCategoryService {

    private final FlowCategoryDAO categories;
    private final FlowCategoryFieldDAO configs;
    private final FlowDefinitionDAO definitions;
    private final FlowModelDAO models;
    private final FlowBusinessFieldsFieldResolver resolver;
    private final FlowBusinessFieldsProperties properties;

    public FlowBusinessFieldsVO.Limits limits() {
        return new FlowBusinessFieldsVO.Limits(properties.getMaxDisplayFields(), properties.getMaxSearchFields());
    }

    public List<FlowBusinessFieldsVO.Category> enrich(List<FlowBusinessFieldsVO.Category> catalog, Long orgId) {
        var saved = configs.selectList(new LambdaQueryWrapper<FlowCategoryField>()
                        .eq(FlowCategoryField::getOrgId, orgId)).stream()
                .collect(Collectors.toMap(FlowCategoryField::getCategoryId, this::fields));
        return catalog.stream().map(category -> {
            var fields = saved.getOrDefault(category.id(), List.of());
            return new FlowBusinessFieldsVO.Category(category.id(), category.code(), category.name(), category.icon(),
                    category.seq(), fields.stream().noneMatch(field -> "OPTION".equals(field.kind()))
                            ? fields : withOptions(fields, snapshots(orgId, category.code())));
        }).toList();
    }

    public FlowBusinessFieldsVO.Configuration get(Long orgId, Long categoryId) {
        FlowCategory category = categories.selectById(categoryId);
        if (category == null || !orgId.equals(category.getOrgId())) {
            throw new BizException("审批分类不存在");
        }
        var snapshots = snapshots(orgId, category.getCode());
        return new FlowBusinessFieldsVO.Configuration(withOptions(fields(config(orgId, categoryId)), snapshots),
                candidates(snapshots.active()), limits());
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void save(Long categoryId, List<Field> requested, LoginUser actor) {
        Long orgId = actor.getOrgId();
        FlowCategory category = categories.selectForUpdate(orgId, categoryId);
        if (category == null) {
            throw new BizException("审批分类不存在");
        }
        var existing = config(orgId, categoryId);
        var old = fields(existing).stream().collect(Collectors.toMap(Field::field, field -> field));
        var available = candidates(snapshots(orgId, category.getCode()).active()).stream()
                .collect(Collectors.toMap(Field::field, field -> field));
        Set<String> seen = new HashSet<>();
        List<Field> updated = new ArrayList<>();
        if (requested == null) {
            throw new BizException("分类字段配置不能为空");
        }
        for (Field item : requested) {
            if (item == null || item.field() == null || item.field().isBlank()) {
                throw new BizException("列表字段标识不能为空");
            }
            String code = FlowBusinessFieldsFieldResolver.normalize(item.field());
            if (!seen.add(code)) {
                throw new BizException("列表字段配置重复：" + code);
            }
            if (!item.displayEnabled() && !item.searchEnabled()) {
                continue;
            }
            Field candidate = available.get(code);
            Field previous = old.get(code);
            if (candidate == null && (previous == null || item.displayEnabled() && !previous.displayEnabled()
                    || item.searchEnabled() && !previous.searchEnabled())) {
                throw new BizException("字段不在当前可选范围：" + code);
            }
            Field descriptor = candidate == null ? previous : candidate;
            String title = item.title() == null ? "" : item.title().strip();
            if (title.isEmpty()) {
                throw new BizException("列表字段名称不能为空：" + code);
            }
            updated.add(new Field(code, title, descriptor.kind(), descriptor.variableType(),
                    item.displayEnabled(), item.searchEnabled(), item.seq(), List.of()));
        }
        if (updated.stream().filter(Field::displayEnabled).count() > properties.getMaxDisplayFields()
                || updated.stream().filter(Field::searchEnabled).count() > properties.getMaxSearchFields()) {
            throw new BizException("列表字段数量超过当前配置上限");
        }
        updated.sort(Comparator.comparing(Field::seq, Comparator.nullsLast(Comparator.naturalOrder())));
        if (existing == null) {
            existing = new FlowCategoryField();
            existing.setOrgId(orgId);
            existing.setCategoryId(categoryId);
            existing.setCreator(actor.getId());
            existing.setCreateTime(LocalDateTime.now());
        }
        existing.setFields(updated);
        existing.setUpdater(actor.getId());
        existing.setUpdateTime(LocalDateTime.now());
        if (existing.getId() == null) {
            configs.insert(existing);
        } else {
            configs.updateById(existing);
        }
    }

    public void validatePublication(Publication publication, FlowVariableSchema schema) {
        var snapshot = snapshots(publication.orgId(), publication.category());
        Map<String, Descriptor> next = resolver.descriptors(schema);
        for (FlowDefinition definition : snapshot.history()) {
            Map<String, Descriptor> past = resolver.published(definition.getVarInfo());
            next.forEach((code, value) -> {
                Descriptor earlier = past.get(code);
                if (earlier != null && !sameType(value, earlier)) {
                    throw new BizException("审批列表字段类型不能变更：" + code);
                }
            });
        }
        List<FlowVariableSchema> active = new ArrayList<>();
        snapshot.active().stream().filter(definition -> !publication.modelId().equals(definition.getModelId()))
                .map(FlowDefinition::getVarInfo).forEach(active::add);
        active.add(schema);
        for (var field : fields(config(publication.orgId(), publication.categoryId()))) {
            var resolved = resolve(field.field(), active);
            if (!resolved.compatible() || resolved.field() != null
                    && (!field.kind().equals(resolved.field().kind().name())
                    || !Objects.equals(field.variableType(), resolved.field().variableType()))) {
                throw new BizException("发布与分类已启用的审批列表字段不兼容：" + field.field());
            }
        }
    }

    public List<Field> configured(Long orgId, String category) {
        return fields(configsForCategory(orgId, category));
    }

    public List<Field> searchable(Long orgId, String category) {
        var fields = configured(orgId, category).stream().filter(Field::searchEnabled).toList();
        return fields.stream().noneMatch(field -> "OPTION".equals(field.kind()))
                ? fields : withOptions(fields, snapshots(orgId, category));
    }

    private FlowCategoryField configsForCategory(Long orgId, String category) {
        FlowCategory match = categories.selectOne(new LambdaQueryWrapper<FlowCategory>()
                .eq(FlowCategory::getOrgId, orgId).eq(FlowCategory::getCode, category));
        return match == null ? null : config(orgId, match.getId());
    }

    private FlowCategoryField config(Long orgId, Long categoryId) {
        return configs.selectOne(new LambdaQueryWrapper<FlowCategoryField>()
                .eq(FlowCategoryField::getOrgId, orgId).eq(FlowCategoryField::getCategoryId, categoryId));
    }

    private List<Field> fields(FlowCategoryField config) {
        return config == null || config.getFields() == null ? List.of() : config.getFields();
    }

    private Snapshots snapshots(Long orgId, String category) {
        var history = definitions.selectList(new LambdaQueryWrapper<FlowDefinition>()
                .select(FlowDefinition::getId, FlowDefinition::getModelId, FlowDefinition::getVarInfo)
                .eq(FlowDefinition::getOrgId, orgId).eq(FlowDefinition::getCategory, category)
                .orderByDesc(FlowDefinition::getId)).stream()
                .filter(definition -> definition.getVarInfo() != null
                        && Objects.equals(definition.getVarInfo().getListVersion(), 1)).toList();
        Set<Long> activeIds = models.selectList(new LambdaQueryWrapper<FlowModel>()
                        .select(FlowModel::getActiveDefinitionId).eq(FlowModel::getOrgId, orgId)
                        .isNotNull(FlowModel::getActiveDefinitionId)).stream()
                .map(FlowModel::getActiveDefinitionId).collect(Collectors.toSet());
        return new Snapshots(history.stream().filter(definition -> activeIds.contains(definition.getId())).toList(), history);
    }

    private List<Field> candidates(List<FlowDefinition> active) {
        var schemas = active.stream().map(FlowDefinition::getVarInfo).toList();
        return schemas.stream().flatMap(schema -> resolver.descriptors(schema).keySet().stream()).distinct()
                .map(code -> resolve(code, schemas))
                .filter(Resolution::compatible).filter(item -> item.field() != null)
                .map(item -> {
                    var field = item.field();
                    return new Field(field.field(), field.title(), field.kind().name(), field.variableType(),
                            false, false, null, item.options());
                }).toList();
    }

    private List<Field> withOptions(List<Field> configured, Snapshots snapshots) {
        return configured.stream().map(field -> {
            if (!"OPTION".equals(field.kind())) {
                return field;
            }
            var options = new LinkedHashMap<String, Option>();
            for (var definition : snapshots.active()) {
                addOptions(options, field, definition.getVarInfo(), false);
            }
            for (var definition : snapshots.history()) {
                addOptions(options, field, definition.getVarInfo(), true);
            }
            return new Field(field.field(), field.title(), field.kind(), field.variableType(),
                    field.displayEnabled(), field.searchEnabled(), field.seq(), List.copyOf(options.values()));
        }).toList();
    }

    private void addOptions(Map<String, Option> options, Field field, FlowVariableSchema schema, boolean historical) {
        Descriptor descriptor = resolver.published(schema).get(field.field());
        if (descriptor != null && field.kind().equals(descriptor.kind().name())
                && Objects.equals(field.variableType(), descriptor.variableType())) {
            descriptor.options().forEach(option -> options.putIfAbsent(option.value(),
                    new Option(option.value(), option.label(), historical)));
        }
    }

    private Resolution resolve(String code, List<FlowVariableSchema> schemas) {
        Descriptor selected = null;
        Map<String, Option> options = new LinkedHashMap<>();
        for (var schema : schemas) {
            var descriptor = resolver.descriptors(schema).get(code);
            if (descriptor == null) {
                boolean privateField = schema.getFields().stream()
                        .anyMatch(field -> FlowBusinessFieldsFieldResolver.normalize(field.getField()).equals(code));
                if (privateField) {
                    return new Resolution(false, null, List.of());
                }
                continue;
            }
            if (selected != null && !sameType(selected, descriptor)) {
                return new Resolution(false, null, List.of());
            }
            selected = descriptor;
            for (Option option : descriptor.options()) {
                Option previous = options.putIfAbsent(option.value(), option);
                if (previous != null && !Objects.equals(previous.label(), option.label())) {
                    return new Resolution(false, null, List.of());
                }
            }
        }
        return new Resolution(true, selected, List.copyOf(options.values()));
    }

    private boolean sameType(Descriptor first, Descriptor second) {
        return first.kind() == second.kind() && Objects.equals(first.variableType(), second.variableType());
    }

    private record Snapshots(List<FlowDefinition> active, List<FlowDefinition> history) {
    }

    private record Resolution(boolean compatible, Descriptor field, List<Option> options) {
    }
}
