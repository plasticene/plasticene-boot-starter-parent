package com.plasticene.boot.flow.core.entity;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.mybatis.core.handlers.type.LongListTypeHandler;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


/**
 *
 * <p> 流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "flow_model", autoResultMap = true)
public class FlowModel extends BaseDO {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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

    /** 流程状态  0：草稿  1：发布  -1：停用 */
    @TableField("status")
    private Integer status;

    /** 流程表单id */
    @TableField("form_id")
    private Long formId;

    /** 设计态-流程模型节点配置 */
    @TableField(value = "model_node", typeHandler = JacksonTypeHandler.class)
    private FlowNode modelNode;

    /** 运行态-流程模型节点配置 */
    @TableField(value = "active_model", typeHandler = JacksonTypeHandler.class)
    private FlowNode activeModel;

    /** 当前发布的模型定义id */
    @TableField("active_definition_id")
    private Long activeDefinitionId;

    /** 当前发布版本 */
    @TableField("active_version")
    private Integer activeVersion;

    /** 发布时间 */
    @TableField("publish_time")
    private LocalDateTime publishTime;

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

    /** 是否编辑过 0：否  1：是 */
    @TableField("is_edited")
    private Integer isEdited;
}
