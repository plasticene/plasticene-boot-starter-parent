package com.plasticene.boot.flow.core.model.vo;

import com.plasticene.boot.flow.core.enums.FlowAddSignEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 展示加签批次、参与人员及执行结果。
 * @author ZFJ
 * @since 2026-11-03
 */
@Data
public class FlowTaskAddSignVO {
    /** 加签批次ID。 */
    private Long id;
    /** 发起加签的原审批任务ID。 */
    private Long sourceTaskId;
    /** 所属审批节点key。 */
    private String nodeKey;
    /** 加签发起人ID。 */
    private Long initiatorId;
    /** 加签发起人名称。 */
    private String initiatorName;
    /** 加签类型。 */
    private FlowAddSignEnum.Type signType;
    /** 加签审批方式。 */
    private Integer approveMode;
    /** 加签人员ID有序列表。 */
    private List<Long> assigneeIds;
    /** 与人员ID顺序一致的名称列表。 */
    private List<String> assigneeNames;
    /** 加签原因。 */
    private String comment;
    /** 加签批次状态。 */
    private FlowAddSignEnum.State state;
    /** 发起时间。 */
    private LocalDateTime createTime;
    /** 批次结束时间。 */
    private LocalDateTime endTime;
}
