package com.plasticene.boot.flow.core.model.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

/**
 * 当前审批任务的表单字段修改。
 * @author ZFJ
 * @since 2026-11-04
 */
@Data
public class FlowTaskFormParam {
    @NotNull(message = "审批任务ID不能为空")
    @Schema(description = "审批任务ID")
    private Long taskId;
    @NotNull(message = "本次修改的表单字段不能为空")
    @Schema(description = "本次修改的表单字段")
    private Map<String, Object> formData;
}
