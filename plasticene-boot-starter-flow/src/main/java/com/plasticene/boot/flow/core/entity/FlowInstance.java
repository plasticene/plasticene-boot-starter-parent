package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FlowInstance extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 公司id
     */
    private Long orgId;
    /**
     * 申请人
     */
    private Long userId;
    /**
     * 流程模型id
     */
    private Long processId;
    /**
     * 状态 0：审批中  1：审批通过  2：审批拒绝
     */
    private Integer status;
    /**
     * 开始时间
     */
    private Date startTime;
    /**
     * 结束时间
     */
    private Date endTime;
    /**
     * 当前节点key
     */
    private String currentNodeKey;
    /**
     * 当前节点名称
     */
    private String currentNodeName;
    /**
     * 业务id
     */
    private Long businessId;

    /**
     * 模型快照
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ProcessNode model;

    /**
     * 表单配置
     */
    private String form;

    /**
     * 参数变量值
     */
    private String varMap;

    /**
     * 流程分类
     */
    private String category;
}
