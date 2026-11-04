package com.plasticene.boot.flow.core.model.param;

import com.plasticene.boot.flow.core.enums.FlowAddSignEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 接收审批任务前加签、后加签和并加签请求。
 * @author ZFJ
 * @since 2026-11-03
 */
@Data
public class FlowTaskAddSignParam {
    @Schema(description = "当前用户发起加签的待办ID")
    @NotNull
    @Positive
    private Long taskId;
    @Schema(description = "加签类型")
    @NotNull
    private FlowAddSignEnum.Type type;
    @Schema(description = "加签人员ID有序列表")
    @NotEmpty
    private List<@NotNull @Positive Long> assigneeIds;
    @Schema(description = "前后加签组的审批方式；并加签由服务端使用当前节点方式")
    private Integer approveMode;
    @Schema(description = "必填加签原因，最多500字；后加签同时作为本人同意意见")
    @NotBlank
    @Size(max = 500)
    private String comment;
}
