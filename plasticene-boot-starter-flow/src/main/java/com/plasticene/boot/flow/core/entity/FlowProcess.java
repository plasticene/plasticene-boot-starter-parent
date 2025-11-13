package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.mybatis.core.handlers.type.LongListTypeHandler;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流-流程模型
 * @author ZFJ
 * @date 2025/9/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "flow_process", autoResultMap = true)
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
     * 流程状态 0：草稿  1：已发布  2：历史
     */
    private Integer status;
    /**
     * 流程说明
     */
    private String remark;

    /**
     * 流程表单id
     */
    private Long formId;
    /**
     * 流程模型配置
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ProcessNode processNode;

    /**
     * 可发起用户
     */
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> startUserIds;
    /**
     * 可发起部门
     */
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> startDeptIds;
    /**
     * 可发起角色
     */
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> startRoleIds;
    /**
     * 管理员
     */
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> managerUserIds;

    /**
     * 发布版本 从1开始
     */
    private Integer version;

    /**
     * 是否启用 0：否  1：是
     */
    private Integer enable;

    /**
     * 发布时间
     */
    private LocalDateTime releaseTime;

}
