package com.plasticene.boot.flow.core.dto;

import lombok.Data;

import java.util.List;

/**
 * 条件节点规则组
 * @author ZFJ
 * @date 2025/9/2
 */
@Data
public class ProcessConditionGroup {
    /**
     * 条件关系 0:and  1:or
     */
    private Integer type;

    /**
     * 多个条件构成一个条件组
     */
    private List<ProcessConditionRule> conditionRules;
}
