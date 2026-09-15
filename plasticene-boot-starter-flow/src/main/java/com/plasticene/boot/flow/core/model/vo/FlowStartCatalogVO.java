package com.plasticene.boot.flow.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 发起审批流程目录
 *
 * @author ZFJ
 * @since 2026-09-14
 */
@Data
@Schema(description = "发起审批流程目录")
public class FlowStartCatalogVO {
    @Schema(description = "当前用户可发起的流程")
    private List<FlowStartableModelVO> models;
    @Schema(description = "最近使用的流程模型id，按使用时间倒序")
    private List<Long> recentModelIds;
}
