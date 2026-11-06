package com.plasticene.boot.flow.core.model.param;

import com.plasticene.boot.web.core.validator.Insert;
import com.plasticene.boot.web.core.validator.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 接收流程表单保存参数。
 * @author ZFJ
 * @since 2026-11-05
 */
@Data
public class FlowFormParam {
    @Schema(description = "表单id")
    @NotNull(message = "id不能为空", groups = Update.class)
    private Long id;
    @Schema(description = "表单名称")
    @NotBlank(message = "表单名称不能为空", groups = Insert.class)
    private String name;
    @Schema(description = "前端内置图标标识，空值使用默认图标")
    @Size(max = 64, message = "图标标识长度不能超过64个字符", groups = {Insert.class, Update.class})
    private String icon;
    @Schema(description = "状态 0：关闭  1：开启")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "表单配置")
    private Map<String, Object> formConfig;
    @Schema(description = "表单分类编码，关联流程分类")
    @NotBlank(message = "表单分类不能为空", groups = Insert.class)
    @Size(max = 32, message = "分类长度不能超过32个字符", groups = {Insert.class, Update.class})
    private String category;
}
