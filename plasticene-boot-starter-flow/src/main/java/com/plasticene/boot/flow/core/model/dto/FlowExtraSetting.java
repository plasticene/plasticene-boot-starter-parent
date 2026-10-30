package com.plasticene.boot.flow.core.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程更多设置
 *
 * @author ZFJ
 * @since 2026-09-28
 */
@Data
@Schema(description = "流程更多设置")
public class FlowExtraSetting {

    @Schema(description = "允许提交人撤销审批中的申请")
    private Boolean allowSubmitterCancel = true;

    @Schema(description = "允许审批人撤回已同意的审批结果")
    private Boolean allowApproverWithdraw = false;

    @Schema(description = "审批人去重策略")
    private ApproverDeduplication approverDeduplication = ApproverDeduplication.NONE;

    @Schema(description = "流程默认超时规则，缺省关闭")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private FlowTimeoutRule timeoutRule;

    public static FlowExtraSetting normalize(FlowExtraSetting setting) {
        FlowExtraSetting normalized = setting == null ? new FlowExtraSetting() : setting;
        if (normalized.getAllowSubmitterCancel() == null) {
            normalized.setAllowSubmitterCancel(false);
        }
        if (normalized.getAllowApproverWithdraw() == null) {
            normalized.setAllowApproverWithdraw(false);
        }
        if (normalized.getApproverDeduplication() == null) {
            normalized.setApproverDeduplication(ApproverDeduplication.NONE);
        }
        return normalized;
    }

    /**
     * 审批人去重策略
     */
    public enum ApproverDeduplication {
        /** 不自动通过 */
        NONE,
        /** 整个流程仅审批一次 */
        ONCE_PER_PROCESS,
        /** 仅连续审批节点自动通过 */
        CONSECUTIVE
    }
}
