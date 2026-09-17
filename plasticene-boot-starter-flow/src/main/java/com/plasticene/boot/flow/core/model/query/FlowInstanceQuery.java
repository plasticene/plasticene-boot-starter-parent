package com.plasticene.boot.flow.core.model.query;

import com.plasticene.boot.common.pojo.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 流程实例分页查询参数
 *
 * @author ZFJ
 * @since 2026-09-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程实例分页查询参数")
public class FlowInstanceQuery extends PageQuery {

    @Schema(description = "关键字，搜索流程名称、编码或者实例id")
    private String keyword;

    @Schema(description = "关键字解析出的流程实例id", hidden = true)
    private Long keywordInstanceId;

    @Schema(description = "申请人id")
    private Long startUserId;

    @Schema(description = "流程分类编码")
    private String category;

    @Schema(description = "当前节点名称")
    private String currentNodeName;

    @Min(value = 0, message = "流程实例状态最小值为 0")
    @Max(value = 3, message = "流程实例状态最大值为 3")
    @Schema(description = "实例状态 0：审批中 1：审批通过 2：审批拒绝 3：已取消")
    private Integer status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "发起时间开始值")
    private LocalDateTime startTimeBegin;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "发起时间结束值")
    private LocalDateTime startTimeEnd;

    @Schema(description = "申请人id", hidden = true)
    private Long userId;

    @Schema(description = "租户id", hidden = true)
    private Long orgId;
}
