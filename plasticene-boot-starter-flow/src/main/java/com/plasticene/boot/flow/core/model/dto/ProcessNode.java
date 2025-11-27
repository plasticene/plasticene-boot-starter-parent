package com.plasticene.boot.flow.core.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * 流程节点
 * @author ZFJ
 * @date 2025/9/2
 */
@Data
// 解决toString循环问题
@ToString(exclude = {"parentNode"})
// 序列化的时候忽略null字段，解决数据库很多null无效字段信息
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessNode {

    /**
     * 节点key
     */
    @Schema(description = "节点key")
    private String key;

    /**
     * 节点名称
     */
    @Schema(description = "节点名称")
    private String name;

    /**
     * 节点类型
     * -1：结束节点   0：发起人   1：审批人   2：抄送人   3：条件节点   4：条件分支 <br>
     * 关联枚举类{@link com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum.Type}
     */
    @Schema(description = "节点类型 -1：结束节点 0：发起人 1：审批人 2：抄送人 3：条件节点 4：条件分支")
    private Integer type;

    /**
     * 节点参与者类型 0：指定人员  1：主管   2：连续多级主管  3：角色
     */
    @Schema(description = "节点处理人类型 0：指定人员 1：主管 2：连续多级主管 3：角色")
    private Integer assigneeType;

    /**
     * 节点参与者 如审批节点、抄送节点指定的人员
     */
    @Schema(description = "处理人")
    private List<Long> assigneeList;

    /**
     * 参与者为主管时，指定主管等级，如 1：发起人的直接主管
     */
    @Schema(description = "审批主管层级")
    private Integer leaderLevel;

    /**
     * 参与者为连续多级主管时，指定当前节点审批结束等级  如 1：发起人的直接主管审批之后流入下一个节点
     * 0：表示连续审批到最上层主管
     */
    @Schema(description = "结束主管层级")
    private Integer leaderEndLevel;

    /**
     * 当审批人与提交人同一人时处理方式
     * 0：自己审批  1：自动跳过 2：转给直属上级审批  3：转给部门 负责人审批 <br>
     * {@link com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum.SelfApprove}
     */
    @Schema(description = "当审批人与提交人同一人时处理方式 0：自己审批  1：自动跳过 2：转给直属上级审批  3：转给部门负责人审批 ")
    private Integer selfApprove;

    /**
     * 当审批人为空时处理方式
     * 0：自动通过  1：自动拒绝 2：指定人员审批  3：转给流程管理员<br>
     * {@link com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum.AssigneeEmpty}
     */
    @Schema(description = "当审批人为空时处理方式 0：自动通过  1：自动拒绝 2：指定人员审批  3：转给流程管理员")
    private Integer assigneeEmpty;
    /**
     * 当审批人为空时指定的人员
     */
    @Schema(description = "当审批人为空时指定的人员")
    private List<Long> emptyUserIds;

    /**
     * 审批类型 0：人工审批  1：自动通过 2：自动拒绝
     */
    @Schema(description = "审批类型 0：人工审批 1：自动通过 2：自动拒绝")
    private Integer approveType;

    /**
     * 多人审批方式
     * 0：会签 (同时审批，每个人必须审批通过)
     * 1：或签 (有一人审批通过即可流入下一个节点)
     * 2：按顺序依次审批(此方式一个审批节点等同于包含多个审批节点，这种配置可以有效降低流程模型的高度)
     */
    @Schema(description = "多人审批方式 0：会签  1：或签  2：顺序审批")
    private Integer approveMode;

    /**
     * 是否需要填写审批意见  0：否  1：是
     */
    @Schema(description = "是否需要填写审批意见 0：否 1：是")
    private Integer requireComment;

    /**
     * 子节点
     */
    @Schema(description = "子节点")
    private ProcessNode childNode;

    /**
     * 条件节点
     */
    @Schema(description = "条件节点集合")
    private List<ProcessNodeCondition> conditionNodes;

    /**
     * 父节点 逻辑字段后端用
     */
    @Schema(description = "父节点  后端使用，前端不需要传")
    private ProcessNode parentNode;
}

