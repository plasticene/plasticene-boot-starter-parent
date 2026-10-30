package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;
import com.plasticene.boot.mybatis.core.handlers.type.LongListTypeHandler;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "flow_task", autoResultMap = true)
public class FlowTask extends BaseDO {
    /**
     * 审批任务主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 公司id
     */
    private Long orgId;
    /**
     * 流程实例id
     */
    private Long instanceId;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 节点key
     */
    private String nodeKey;
    /**
     * 节点类型
     */
    private Integer nodeType;
    /**
     * 处理人
     */
    private Long assignee;
    /**
     * 审批类型 0：人工审批 1：自动通过 2：自动拒绝
     */
    private Integer approveType;
    /**
     * 多人审批方式 0：会签  1：或签  2：顺序审批
     */
    private Integer approveMode;
    /**
     * 状态 0：处理中  1：已完成  2：拒绝  3：已取消
     */
    private Integer status;
    /**
     * 是否删除  0：否  1；是
     */
    private Integer isDelete;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 审批意见
     */
    private String comment;

    /**
     * 是否需要填写审批意见  0：否  1：是
     */
    private Integer requireComment;

    /**
     * 并行分支完成的分支数
     */
    private Integer completedBranch;
    /**
     * 多人顺序审批时存放处理人
     */
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> assigneeList;

    /**
     * 待办生效的超时规则快照；由发布版本的流程默认规则或节点自定义规则解析得到。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private FlowTimeoutRule timeoutRule;
    /**
     * 自动处理截止时间，从当前待办创建时刻计算；未开启自动处理时为空。
     */
    private LocalDateTime deadline;
    /**
     * 下次计划生成业务提醒的时间；未开启提醒、轮次用尽或超时已处理时为空。
     */
    private LocalDateTime nextRemindTime;
    /**
     * 已生成的业务提醒轮次，初始为 0；与渠道是否发送成功、发送重试次数无关。
     */
    private Integer remindCount;
    /**
     * 是否由系统超时自动处理：false 否，true 是；用于区分人工审批与系统决定。
     */
    private Boolean timeoutHandled;

}
