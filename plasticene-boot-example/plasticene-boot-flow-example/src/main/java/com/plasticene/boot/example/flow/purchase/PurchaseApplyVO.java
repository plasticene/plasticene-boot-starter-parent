package com.plasticene.boot.example.flow.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购申请列表与详情的响应契约。
 * @author ZFJ
 * @since 2026-11-18
 */
@Schema(description = "采购申请")
public record PurchaseApplyVO(
        @Schema(description = "采购申请 ID") Long id,
        @Schema(description = "采购标题") String title,
        @Schema(description = "采购说明") String description,
        @Schema(description = "采购金额，单位元") BigDecimal amount,
        @Schema(description = "采购类别编码") String category,
        @Schema(description = "采购用途编码集合") List<String> purposes,
        @Schema(description = "是否紧急") Boolean urgent,
        @Schema(description = "期望到货日期") LocalDate expectedDate,
        @Schema(description = "流程实例 ID") Long instanceId,
        @Schema(description = "业务审批状态：0 审批中，1 已通过，2 已拒绝，3 已取消") Integer status,
        @Schema(description = "申请人 ID") Long createdBy,
        @Schema(description = "申请时间") LocalDateTime createdTime) {
    /** 将已保存的业务实体转换为接口响应。 */
    public static PurchaseApplyVO from(PurchaseApply apply) {
        return new PurchaseApplyVO(apply.getId(), apply.getTitle(), apply.getDescription(), apply.getAmount(),
                apply.getCategory(), apply.getPurposes(), apply.getUrgent(), apply.getExpectedDate(),
                apply.getInstanceId(), apply.getStatus(), apply.getCreatedBy(), apply.getCreatedTime());
    }
}
