package com.plasticene.boot.example.flow.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/**
 * 提交采购申请的业务参数。
 * @author ZFJ
 * @since 2026-11-18
 */
@Data
@Schema(description = "采购申请提交参数")
public class PurchaseApplyParam {
    @Schema(description = "同一次提交使用固定 UUID，重试时保持不变")
    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    private String requestNo;
    @Schema(description = "采购标题")
    @NotBlank
    @Size(max = 100)
    private String title;
    @Schema(description = "采购说明")
    @NotBlank
    @Size(max = 1000)
    private String description;
    @Schema(description = "采购金额，单位元")
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;
    @Schema(description = "采购类别：office、electronics、other")
    @NotBlank
    @Pattern(regexp = "office|electronics|other")
    private String category;
    @Schema(description = "采购用途：office、project、replacement")
    @NotEmpty
    @Size(max = 3)
    private List<@NotNull @Pattern(regexp = "office|project|replacement") String> purposes;
    @Schema(description = "是否紧急")
    @NotNull
    private Boolean urgent;
    @Schema(description = "期望到货日期，不得早于提交当天")
    @NotNull
    @FutureOrPresent
    private LocalDate expectedDate;
}
