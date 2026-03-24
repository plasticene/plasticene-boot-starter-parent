package com.plasticene.boot.flow.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流程条件节点条件配置
 * @author ZFJ
 * @date 2025/9/2
 */

@Data
public class ProcessNodeCondition {

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


}
