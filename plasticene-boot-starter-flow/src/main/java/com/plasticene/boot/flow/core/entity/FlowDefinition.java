package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.mybatis.core.handlers.type.LongListTypeHandler;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流程定义表：流程模型发布记录表
 * @author ZFJ
 * @since 2026/4/15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "flow_definition", autoResultMap = true)
public class FlowDefinition extends BaseDO {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 模型id */
    @TableField("model_id")
    private Long modelId;

    /** 公司id */
    @TableField("org_id")
    private Long orgId;

    /** 流程唯一标识(区分大小写) */
    @TableField("code")
    private String code;

    /** 流程名称 */
    @TableField("name")
    private String name;

    /** 流程分类 */
    @TableField("category")
    private String category;

    /** 流程表单id */
    @TableField("form_id")
    private Long formId;

    /** 设计态-流程模型节点配置 */
    @TableField(value = "model_node", typeHandler = JacksonTypeHandler.class)
    private FlowNode modelNode;

    /** 当前发布版本 */
    @TableField("version")
    private Integer version;

    /** 可发起人类型  0：全员   1：指定人员   2：指定部门    3：指定角色 */
    @TableField("start_user_type")
    private Integer startUserType;

    /** 可发起用户 */
    @TableField(value = "start_user_ids", typeHandler = LongListTypeHandler.class)
    private List<Long> startUserIds;

    /** 可发起部门 */
    @TableField(value = "start_dept_ids", typeHandler = LongListTypeHandler.class)
    private List<Long> startDeptIds;

    /** 可发起角色 */
    @TableField(value = "start_role_ids", typeHandler = LongListTypeHandler.class)
    private List<Long> startRoleIds;

    /** 管理员 */
    @TableField(value = "manager_user_ids", typeHandler = LongListTypeHandler.class)
    private List<Long> managerUserIds;

    /** 说明 */
    @TableField("remark")
    private String remark;

    /** 是否删除 0：否  1：是 */
    @TableField("is_delete")
    private Integer isDelete;
}
