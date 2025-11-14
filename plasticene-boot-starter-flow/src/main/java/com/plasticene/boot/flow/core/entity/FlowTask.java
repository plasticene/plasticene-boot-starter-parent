package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowTask extends BaseDO {
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
     * 状态 0：处理中  1：已完成  2：拒绝
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
}
