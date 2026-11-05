package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 返回流程表单详情及分类信息。
 * @author ZFJ
 * @since 2026-11-05
 */
@Data
public class FlowFormVO {
    @Schema(description = "表单id")
    private Long id;
    @Schema(description = "表单名称")
    private String name;
    @Schema(description = "前端内置图标标识，空值使用默认图标")
    private String icon;
    @Schema(description = "状态 0：关闭  1：开启")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "表单配置")
    private Map<String, Object> formConfig;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "创建人")
    private String creator;
    @Schema(description = "更新人")
    private String updater;
    @Schema(description = "表单分类编码，关联流程分类")
    private String category;
    @Schema(description = "表单分类名称")
    private String categoryName;
}
