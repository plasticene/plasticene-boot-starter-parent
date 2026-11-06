package com.plasticene.boot.flow.core.model.param;

import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.web.core.validator.Insert;
import com.plasticene.boot.web.core.validator.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *
 * <p> 工作流-流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

@Data
@Schema(description = "工作流-流程模型表")
public class FlowModelParam {
    @Schema(description = "流程id")
    @NotNull(message = "流程id不能为空", groups = Update.class)
    private Long id;
    @Schema(description = "流程唯一标识，同一组织内唯一、区分大小写，新增或变更时5～32位，以英文字母或数字开头，仅支持英文字母、数字、下划线和连字符")
    @NotBlank(message = "流程唯一标识不能为空", groups = Insert.class)
    @Size(min = 5, max = 32, message = "流程标识须为5～32位", groups = Insert.class)
    @Size(max = 32, message = "流程标识长度不能超过32个字符", groups = Update.class)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$",
            message = "流程标识必须以英文字母或数字开头，仅支持英文字母、数字、下划线和连字符",
            groups = {Insert.class, Update.class})
    private String code;
    @Schema(description = "流程名称")
    @NotBlank(message = "流程名称不能为空", groups = Insert.class)
    @Size(max = 32, message = "名称长度不能超过32个字符")
    private String name;
    @Schema(description = "前端内置图标标识，空值使用默认图标")
    @Size(max = 64, message = "图标标识长度不能超过64个字符", groups = {Insert.class, Update.class})
    private String icon;
    @Schema(description = "流程分类标识")
    @NotBlank(message = "流程分类不能为空", groups = Insert.class)
    @Size(max = 32, message = "分类长度不能超过32个字符", groups = {Insert.class, Update.class})
    private String category;
    @Schema(description = "流程状态  0：草稿  1：发布  -1：停用")
    private Integer status;
    @Schema(description = "流程表单配置快照；更新时省略保持原值，空对象清空表单")
    private Map<String, Object> formConfig;
    @Schema(description = "流程更多设置")
    private FlowExtraSetting extraSetting;
    @Schema(description = "流程模型配置")
    private FlowNode modelNode;
    @Schema(description = "可发起人类型  0：全员   1：指定人员   2：指定部门    3：指定角色")
    private Integer startUserType;
    @Schema(description = "可发起用户")
    private List<Long> startUserIds;
    @Schema(description = "可发起角色")
    private List<Long>startRoleIds;
    @Schema(description = "可发起部门")
    private List<Long>startDeptIds;
    @Schema(description = "管理员")
    private List<Long> managerUserIds;
    @Schema(description = "说明")
    private String remark;
}
