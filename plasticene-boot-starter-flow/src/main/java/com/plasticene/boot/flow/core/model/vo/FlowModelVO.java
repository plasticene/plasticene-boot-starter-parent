package com.plasticene.boot.flow.core.model.vo;

import com.plasticene.boot.flow.core.model.dto.FlowNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * <p> 工作流-流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

@Data
@Schema(description = "工作流-流程模型表")
public class FlowModelVO {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "公司id")
    private Long orgId;
    @Schema(description = "流程唯一标识(区分大小写)")
    private String code;
    @Schema(description = "流程名称")
    private String name;
    @Schema(description = "流程分类")
    private String category;
    @Schema(description = "流程分类名称")
    private String categoryName;
    @Schema(description = "流程状态  0：草稿  1：发布  -1：停用")
    private Integer status;
    @Schema(description = "流程表单id")
    private Long formId;
    @Schema(description = "流程表单配置")
    private FlowNode modelNode;
    @Schema(description = "设计态-流程模型节点配置")
    private String draftModel;
    @Schema(description = "运行态-流程模型节点配置")
    private String activeModel;
    @Schema(description = "当前发布的模型定义id")
    private Long activeDefinitionId;
    @Schema(description = "当前发布版本")
    private Integer activeVersion;
    @Schema(description = "可发起人类型  0：全员   1：指定人员   2：指定部门    3：指定角色")
    private Integer startUserType;
    @Schema(description = "可发起用户")
    private List<Long> startUserIds;
    @Schema(description = "可发起用户名称")
    private List<String> startUserNames;
    @Schema(description = "可发起部门")
    private List<Long> startDeptIds;
    @Schema(description = "可发起部门名称")
    private List<String> startDeptNames;
    @Schema(description = "可发起角色")
    private List<Long> startRoleIds;
    @Schema(description = "可发起角色名称")
    private List<String> startRoleNames;
    @Schema(description = "管理员")
    private List<Long> managerUserIds;
    @Schema(description = "管理员名称")
    private List<String> managerUserNames;
    @Schema(description = "说明")
    private String remark;
}