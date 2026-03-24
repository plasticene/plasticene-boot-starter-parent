package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ZFJ
 * @since 2026/2/25
 */
@Data
public class FormVO {
    @Schema(description = "表单id")
    private Long id;
    @Schema(description = "表单名称")
    private String name;
    @Schema(description = "状态 0：关闭  1：开启")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "表单配置")
    private String conf;
    @Schema(description = "表单字段")
    private List<String> fields;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "创建人")
    private String creator;
    @Schema(description = "更新人")
    private String updater;
}
