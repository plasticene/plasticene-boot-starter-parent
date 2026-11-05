package com.plasticene.boot.flow.core.model.vo;

import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author ZFJ
 * @since 2026/9/15
 */
@Data
@Schema(description = "流程定义VO")
public class FlowDefinitionVO {

    @Schema(description = "流程模型id")
    private Long modelId;
    @Schema(description = "发布定义id")
    private Long definitionId;
    @Schema(description = "流程编码")
    private String code;
    @Schema(description = "流程名称")
    private String name;
    @Schema(description = "流程分类编码")
    private String category;
    @Schema(description = "流程分类名称")
    private String categoryName;
    @Schema(description = "流程说明")
    private String remark;
    @Schema(description = "发布版本")
    private Integer version;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "发布时的表单配置快照")
    private Map<String, Object> formConfig;
    @Schema(description = "发布时的流程更多设置快照")
    private FlowExtraSetting extraSetting;
    @Schema(description = "发布时的流程模型快照")
    private FlowNode modelNode;

    @Schema(description = "可发起人类型：0 全员，1 指定人员，2 指定部门，3 指定角色")
    private Integer startUserType;
    @Schema(description = "可发起用户id")
    private List<Long> startUserIds;
    @Schema(description = "可发起部门id")
    private List<Long> startDeptIds;
    @Schema(description = "可发起角色id")
    private List<Long> startRoleIds;
    @Schema(description = "流程管理员id")
    private List<Long> managerUserIds;
    @Schema(description = "发布人名称，发布历史列表返回")
    private String publisherName;
    @Schema(description = "是否为模型当前关联的发布版本，发布历史列表返回；停用模型暂不可发起")
    private Boolean active;

}
