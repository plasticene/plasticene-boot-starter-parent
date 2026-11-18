package com.plasticene.boot.example.flow.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 采购申请示例的单表业务实体。
 * @author ZFJ
 * @since 2026-11-18
 */
@Data
public class PurchaseApply {
    /** 采购申请 ID。 */
    private Long id;
    /** 所属组织 ID。 */
    private Long orgId;
    /** 提交请求标识，用于重复提交识别。 */
    private String requestNo;
    /** 采购标题。 */
    private String title;
    /** 采购说明。 */
    private String description;
    /** 采购金额，单位元。 */
    private BigDecimal amount;
    /** 采购类别编码。 */
    private String category;
    /** 采购用途编码集合。 */
    private List<String> purposes;
    /** 是否紧急。 */
    private Boolean urgent;
    /** 期望到货日期。 */
    private LocalDate expectedDate;
    /** 关联审批流程实例 ID。 */
    private Long instanceId;
    /** 业务审批状态：0 审批中，1 已通过，2 已拒绝，3 已取消。 */
    private Integer status;
    /** 申请人 ID。 */
    private Long createdBy;
    /** 申请时间。 */
    private LocalDateTime createdTime;
}
