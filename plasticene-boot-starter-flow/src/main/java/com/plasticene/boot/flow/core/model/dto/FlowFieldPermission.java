package com.plasticene.boot.flow.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程节点表单字段权限配置。
 * @author ZFJ
 * @since 2026-11-04
 */
@Data
public class FlowFieldPermission {
    /** 表单字段标识。 */
    @Schema(description = "表单字段标识")
    private String field;
    /** 表单字段显示名称。 */
    @Schema(description = "表单字段显示名称")
    private String title;
    /** 字段权限：READ 只读，WRITE 编辑，HIDDEN 隐藏。 */
    @Schema(description = "字段权限：READ 只读，WRITE 编辑，HIDDEN 隐藏")
    private Permission permission;

    /** 字段权限以枚举名称作为接口和持久化值。 */
    public enum Permission {
        READ, WRITE, HIDDEN
    }
}
