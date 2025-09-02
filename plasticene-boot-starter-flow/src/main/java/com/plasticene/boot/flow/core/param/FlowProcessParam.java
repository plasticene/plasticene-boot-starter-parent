package com.plasticene.boot.flow.core.param;

import com.plasticene.boot.flow.core.dto.ProcessNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Data
@Schema(description = "流程模型参数")
public class FlowProcessParam {

    @Schema(description = "流程唯一标识, 区分大小写的")
    @NotBlank(message = "流程唯一标识不能为空")
    @Size(max = 16, message = "key长度不能超过16个字符")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_\\-.]*$",
            message = "key只能包含字母、数字、下划线、连字符和点号，且必须以字母开头")
    private String code;
    @Schema(description = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    @Size(max = 16, message = "名称长度不能超过16个字符")
    private String name;
    @Schema(description = "流程分类标识")
    @NotBlank(message = "流程分类不能为空")
    @Size(max = 16, message = "分类长度不能超过16个字符")
    private String category;
    @Schema(description = "流程描述")
    private String remark;

    @Schema(description = "表单id")
    private Long formId;
    @Schema(description = "流程模型节点配置")
    private ProcessNode processNode;
    @Schema(description = "流程id")
    private Long processId;
}
