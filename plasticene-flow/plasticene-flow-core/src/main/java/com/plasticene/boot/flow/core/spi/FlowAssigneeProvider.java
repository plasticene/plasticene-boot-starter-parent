package com.plasticene.boot.flow.core.spi;

import com.plasticene.boot.flow.core.model.FlowActor;
import com.plasticene.boot.flow.core.model.FlowDefinition;
import com.plasticene.boot.flow.core.model.FlowNode;

import java.util.List;

/**
 * 根据流程上下文解析审批人的扩展接口。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public interface FlowAssigneeProvider {

    List<String> resolveAssignees(FlowActor actor, FlowDefinition definition, FlowNode node);
}
