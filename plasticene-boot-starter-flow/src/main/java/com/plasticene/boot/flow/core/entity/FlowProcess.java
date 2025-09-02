package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 工作流-流程模型
 * @author ZFJ
 * @date 2025/9/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FlowProcess extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 公司id
     */
    private Long orgId;
    /**
     * 流程唯一标识，字母区分大小写
     * 只能包含字母、数字、下划线、连字符和点号，且必须以字母开头
     */
    private String code;
    /**
     * 流程名称
     */
    private String name;
    /**
     * 流程分类
     */
    private String category;
    /**
     * 流程版本
     */
    private Integer version;
    /**
     * 流程状态 0：草稿  1：已发布  2：历史
     */
    private Integer status;
    /**
     * 流程发布时间
     */
    private Date releaseTime;
    /**
     * 流程表单id
     */
    private Long formId;
    /**
     * 流程模型配置
     */
    private String model;
    /**
     * 流程说明
     */
    private String remark;

}
