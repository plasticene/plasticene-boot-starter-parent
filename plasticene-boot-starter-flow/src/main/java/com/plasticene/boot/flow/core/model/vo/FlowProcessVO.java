package com.plasticene.boot.flow.core.model.vo;

import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/4
 */
@Data
public class FlowProcessVO {
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "公司id")
    private Long orgId;
    @Schema(description = "流程唯一标识")
    private String code;
    @Schema(description = "流程名称")
    private String name;
    @Schema(description = "流程分类")
    private String category;
    @Schema(description = "流程状态 0：草稿  1：已发布  2：历史")
    private Integer status;
    @Schema(description = "流程说明")
    private String remark;
    @Schema(description = "流程表单id")
    private Long formId;
    @Schema(description = "流程模型配置")
    private ProcessNode processNode;
    @Schema(description = "发起类型 0：全员  1：指定人员")
    private Integer startType;
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
    @Schema(description = "发布版本 从1开始")
    private Integer version;
    @Schema(description = "是否启用 0：否  1：是")
    private Integer enable;
    @Schema(description = "发布时间")
    private LocalDateTime releaseTime;
}
