package com.plasticene.boot.flow.core.entity;

import com.plasticene.boot.flow.core.model.dto.FlowFormFieldChange;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import lombok.EqualsAndHashCode;

/**
 * 保存流程表单每次成功修改的审计快照。
 * @author ZFJ
 * @since 2026-11-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "flow_form_change_record", autoResultMap = true)
public class FlowFormChangeRecord extends BaseDO {
    /** 表单变更记录ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属组织ID。 */
    private Long orgId;
    /** 所属流程实例ID。 */
    private Long instanceId;
    /** 操作所属审批任务ID。 */
    private Long taskId;
    /** 操作所属流程节点标识。 */
    private String nodeKey;
    /** 操作时的流程节点名称快照。 */
    private String nodeName;
    /** 操作人ID。 */
    private Long operatorId;
    /** 操作时的人员姓名快照。 */
    private String operatorName;
    /** 保存成功的业务操作时间。 */
    private LocalDateTime operationTime;
    /** 发生变化的字段明细快照。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<FlowFormFieldChange> fieldDetails;
}
