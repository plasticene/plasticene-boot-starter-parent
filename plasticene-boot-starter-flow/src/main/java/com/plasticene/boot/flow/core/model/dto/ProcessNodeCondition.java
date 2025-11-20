package com.plasticene.boot.flow.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流程条件节点
 * @author ZFJ
 * @date 2025/9/2
 */

@Data
public class ProcessNodeCondition {

    /**
     * 节点key
     */
    @Schema(description = "节点key")
    private String key;

    /**
     * 节点名称
     */
    @Schema(description = "节点name")
    private String name;

    /**
     * 条件组间关系 0:and  1:or
     */
    @Schema(description = "条件组间关系 0:and  1:or")
    private Integer type;

    /**
     * 条件组，一个条件节点包含多个条件组
     */
    @Schema(description = "条件组集合")
    private List<ProcessConditionGroup> conditionGroups;

    /**
     * 子节点
     */
    @Schema(description = "条件节点的子节点")
    private ProcessNode childNode;



}
