package com.plasticene.boot.flow.core.impl.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.plasticene.boot.flow.core.model.enums.FlowAddSignEnum;
import com.plasticene.boot.mybatis.core.handlers.type.LongListTypeHandler;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 保存审批任务加签配置、来源及执行结果。
 * @author ZFJ
 * @since 2026-11-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "flow_task_add_sign", autoResultMap = true)
public class FlowTaskAddSign extends BaseDO {
    /** 加签批次ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属组织ID。 */
    private Long orgId;
    /** 所属流程实例ID。 */
    private Long instanceId;
    /** 加签所属审批节点key。 */
    private String nodeKey;
    /** 发起本次加签的原审批任务ID。 */
    private Long sourceTaskId;
    /** 加签发起人ID。 */
    private Long initiatorId;
    /** 加签类型：BEFORE前加签，AFTER后加签，PARALLEL并加签。 */
    private FlowAddSignEnum.Type signType;
    /** 本批次审批方式：0会签，1或签，2顺序审批；并加签记录节点方式。 */
    private Integer approveMode;
    /** 本批次加签人员ID有序列表，逗号分隔，顺序审批按列表顺序执行。 */
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> assigneeIds;
    /** 加签原因；后加签同时作为原审批人的同意意见。 */
    private String comment;
    /** 批次状态：RUNNING处理中，PASSED已通过，REJECTED已拒绝，CANCELLED已取消，APPLIED并加签已加入。 */
    private FlowAddSignEnum.State state;
    /** 原任务开始暂停的时间；仅前后加签填写，用于恢复剩余超时。 */
    private LocalDateTime pausedAt;
    /** 批次结束时间；并加签为人员加入完成时间。 */
    private LocalDateTime endTime;
}
