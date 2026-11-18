package com.plasticene.boot.flow.core.api.param;

import com.plasticene.boot.flow.core.model.enums.FlowAddSignEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "待办ID不能为空")
    private Long taskId;
    @Schema(description = "加签类型")
    @NotNull(message = "加签类型不能为空")
    private FlowAddSignEnum.Type type;
    @Schema(description = "加签人员ID有序列表")
    @NotEmpty(message = "加签人员ID列表不能为空")
    private List<@NotNull Long> assigneeIds;
    @Schema(description = "前后加签组的审批方式；并加签由服务端使用当前节点方式")
    private Integer approveMode;
    @Schema(description = "必填加签原因，最多500字；后加签同时作为本人同意意见")
    @NotBlank(message = "加签原因不能为空")
    @Size(max = 500)
    private String comment;
}
