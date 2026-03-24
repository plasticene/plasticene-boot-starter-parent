package com.plasticene.boot.flow.core.model.param;

import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.web.core.validator.Insert;
import com.plasticene.boot.web.core.validator.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 流程模型参数
 * @author ZFJ
 * @date 2025/9/2
 */
@Data
@Schema(description = "流程模型参数")
public class FlowProcessParam {

    @Schema(description = "流程唯一标识, 区分大小写的")
    @NotBlank(message = "流程唯一标识不能为空", groups = Insert.class)
    @Size(max = 16, message = "key长度不能超过16个字符", groups = Insert.class)
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_\\-.]*$",
            message = "code只能包含字母、数字、下划线、连字符和点号，且必须以字母开头",
            groups = Insert.class)
    private String code;
    @Schema(description = "流程名称")
    @NotBlank(message = "流程名称不能为空", groups = Insert.class)
    @Size(max = 32, message = "名称长度不能超过32个字符")
    private String name;
    @Schema(description = "流程分类标识")
    @NotBlank(message = "流程分类不能为空", groups = Insert.class)
    @Size(max = 16, message = "分类长度不能超过16个字符", groups = Insert.class)
    private String category;
    @Schema(description = "流程描述")
    private String remark;

    @Schema(description = "表单id")
    private Long formId;
    @Schema(description = "流程模型节点配置")
    private ProcessNode processNode;

    @Schema(description = "发起方式 0：全员 1：指定人员  2：指定部门  3：指定角色")
    private Integer startUserType;
    @Schema(description = "可发起用户")
    private List<Long> startUserIds;
    @Schema(description = "可发起部门")
    private List<Long> startDeptIds;
    @Schema(description = "可发起角色")
    private List<Long> startRoleIds;
    @Schema(description = "流程模型管理员")
    private List<Long> managerUserIds;


    @Schema(description = "流程id")
    @NotNull(message = "流程id不能为空", groups = Update.class)
    private Long id;
}
