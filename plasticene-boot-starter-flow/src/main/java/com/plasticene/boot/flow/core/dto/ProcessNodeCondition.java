package com.plasticene.boot.flow.core.dto;

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
    private String key;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 条件组间关系 0:and  1:or
     */
    private Integer type;

    /**
     * 条件组
     */
    private List<ProcessConditionGroup> conditionGroups;

    /**
     * 子节点
     */
    private ProcessNode childNode;



}
