package com.plasticene.boot.flow.core.model.vo;

import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例任务记录
 *
 * @author ZFJ
 * @since 2026-09-15
 */
@Data
@Schema(description = "流程实例任务记录")
public class FlowTaskVO {
    @Schema(description = "任务id")
    private Long id;
    @Schema(description = "节点key")
    private String nodeKey;
    @Schema(description = "节点名称")
    private String nodeName;
    @Schema(description = "节点类型")
    private Integer nodeType;
    @Schema(description = "处理人id")
    private Long assignee;
    @Schema(description = "处理人名称")
    private String assigneeName;
    @Schema(description = "转交来源任务ID；非转交创建的任务为空")
    private Long sourceTaskId;
    @Schema(description = "转交接收人ID；未转交时为空")
    private Long transferTo;
    @Schema(description = "转交接收人名称；未转交时为空")
    private String transferToName;
    @Schema(description = "当前用户是否可转交此待办")
    private Boolean transferable;
    @Schema(description = "审批类型")
    private Integer approveType;
    @Schema(description = "多人审批方式")
    private Integer approveMode;
    @Schema(description = "任务状态：0处理中，1已审批，2拒绝，3已取消，4已转交，5已退回，6等待加签")
    private Integer status;
    @Schema(description = "任务状态名称")
    private String statusName;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @Schema(description = "审批意见")
    private String comment;
    @Schema(description = "耗时，单位毫秒")
    private Long durationMillis;
    @Schema(description = "当前用户是否可以撤回该审批结果")
    private Boolean withdrawable;
    @Schema(description = "超时提醒和结果通知记录")
    private List<FlowTimeoutNotice> timeoutNotices;
    @Schema(description = "自动处理截止时间")
    private LocalDateTime deadline;
    @Schema(description = "下次计划提醒时间")
    private LocalDateTime nextRemindTime;
    @Schema(description = "超时动作 APPROVE/REJECT")
    private String timeoutAction;
    @Schema(description = "是否由系统超时处理")
    private Boolean timeoutHandled;
    @Schema(description = "退回目标节点key")
    private String returnTargetNodeKey;
    @Schema(description = "退回目标节点名称")
    private String returnTargetNodeName;
    @Schema(description = "使该记录失效的退回任务ID；为空表示仍有效")
    private Long returnTaskId;
    @Schema(description = "当前用户是否可退回此待办")
    private Boolean returnable;
    @Schema(description = "当前用户是否可审批此待办")
    private Boolean processable;
    @Schema(description = "当前用户是否可对此待办加签")
    private Boolean addSignable;
    @Schema(description = "创建该任务的加签批次ID；非加签任务为空")
    private Long addSignId;
    @Schema(description = "顺序审批人员队列，用于展示和排除已有审批人")
    private List<Long> assigneeList;
}

