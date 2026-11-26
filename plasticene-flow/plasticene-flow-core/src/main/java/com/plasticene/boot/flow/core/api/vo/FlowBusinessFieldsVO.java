package com.plasticene.boot.flow.core.api.vo;

import java.util.List;

/**
 * 定义审批目录和分类字段配置的响应结构。
 * @author ZFJ
 * @since 2026-11-20
 */
public final class FlowBusinessFieldsVO {

    private FlowBusinessFieldsVO() {
    }

    /**
     * 描述每个分类的动态字段数量上限。
     * @author ZFJ
     * @since 2026-11-20
     */
    public record Limits(int maxDisplayFields, int maxSearchFields) {
    }

    /**
     * 描述部署是否启用了审批业务字段。
     * @author ZFJ
     * @since 2026-11-20
     */
    public record Capabilities(boolean listFieldsEnabled, Limits limits) {
    }

    /**
     * 返回审批中心初始化所需的分类与字段信息。
     * @author ZFJ
     * @since 2026-11-20
     */
    public record Catalog(boolean listFieldsEnabled, Limits limits, List<Category> categories) {
    }

    /**
     * 返回一个流程分类及生效的动态字段。
     * @author ZFJ
     * @since 2026-11-20
     */
    public record Category(Long id, String code, String name, String icon, Integer seq, List<Field> fields) {
    }

    /**
     * 描述归一化后的静态选项。
     * @author ZFJ
     * @since 2026-11-20
     */
    public record Option(String value, String label, boolean historical) {
    }

    /**
     * 描述审批列表中的一个可配置字段。
     * @author ZFJ
     * @since 2026-11-20
     */
    public record Field(String field, String title, String kind, Integer variableType,
                        boolean displayEnabled, boolean searchEnabled, Integer seq, List<Option> options) {
    }

    /**
     * 返回当前分类的配置和最新可选字段。
     * @author ZFJ
     * @since 2026-11-20
     */
    public record Configuration(List<Field> fields, List<Field> availableFields, Limits limits) {
    }
}
