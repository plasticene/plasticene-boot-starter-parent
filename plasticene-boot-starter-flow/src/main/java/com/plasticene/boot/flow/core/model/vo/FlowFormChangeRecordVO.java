package com.plasticene.boot.flow.core.model.vo;

import com.plasticene.boot.flow.core.model.dto.FlowFormFieldChange;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 返回当前用户有权查看的表单变更记录。
 * @author ZFJ
 * @since 2026-11-04
 */
@Data
@Schema(description = "表单变更记录")
public class FlowFormChangeRecordVO {
    @Schema(description = "表单变更记录ID")
    private Long id;
    @Schema(description = "所属流程实例ID")
    private Long instanceId;
    @Schema(description = "操作所属审批任务ID")
    private Long taskId;
    @Schema(description = "操作所属流程节点标识")
    private String nodeKey;
    @Schema(description = "操作时的流程节点名称快照")
    private String nodeName;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Schema(description = "操作时的人员姓名快照")
    private String operatorName;
    @Schema(description = "保存成功的业务操作时间")
    private LocalDateTime operationTime;
    @Schema(description = "发生变化的字段明细快照")
    private List<FlowFormFieldChange> fieldDetails;
}
