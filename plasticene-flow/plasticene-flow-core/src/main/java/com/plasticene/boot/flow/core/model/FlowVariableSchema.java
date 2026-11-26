package com.plasticene.boot.flow.core.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 保存审批变量定义，独立于表单布局。
 * @author ZFJ
 * @since 2026-11-16
 */
@Data
@Schema(description = "发布版本的审批变量定义")
public class FlowVariableSchema {
    @Schema(description = "可用于审批配置的字段定义")
    private List<Field> fields = List.of();

    @Schema(description = "服务端写入的审批列表字段版本", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer listVersion;
    /**
     * 定义一个审批变量的字段契约。
     * @author ZFJ
     * @since 2026-11-16
     */
    @Data
    @Schema(description = "审批变量字段")
    public static class Field {
        @Schema(description = "稳定字段标识")
        private String field;
        @Schema(description = "字段名称")
        private String title;
        @Schema(description = "变量类型：0字符串、1数字、2日期、3集合；非条件内容字段可为空")
        private Integer type;
        @Schema(description = "是否必填")
        private Boolean required;
        @Schema(description = "集合元素类型")
        private Integer elementType;
        @Schema(description = "日期格式")
        private String format;
        @Schema(description = "表单来源控件类型")
        private String componentType;
        @Schema(description = "字段选项展示属性")
        private Map<String, Object> props;
        @Schema(description = "字段选项")
        private List<Map<String, Object>> options;
        @EqualsAndHashCode.Exclude
        @Schema(description = "允许用于审批列表展示或搜索")
        private Boolean allowInList;
    }
}
