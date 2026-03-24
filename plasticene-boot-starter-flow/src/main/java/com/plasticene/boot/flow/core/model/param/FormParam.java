package com.plasticene.boot.flow.core.model.param;

import com.plasticene.boot.web.core.validator.Insert;
import com.plasticene.boot.web.core.validator.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author ZFJ
 * @since 2026/2/24
 */
@Data
public class FormParam {
    @Schema(description = "表单id")
    @NotNull(message = "id不能为空", groups = Update.class)
    private Long id;
    @Schema(description = "表单名称")
    @NotBlank(message = "表单名称不能为空", groups = Insert.class)
    private String name;
    @Schema(description = "状态 0：关闭  1：开启")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "表单配置")
    private String conf;
    @Schema(description = "表单字段")
    private List<String> fields;
}
