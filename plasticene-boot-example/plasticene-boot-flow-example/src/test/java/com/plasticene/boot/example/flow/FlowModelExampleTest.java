package com.plasticene.boot.example.flow;

import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审批流领域模型示例测试。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
class FlowModelExampleTest {

    @Test
    void buildsAFlowWithoutDependingOnWebOrPersistenceTypes() {
        FlowNode end = new FlowNode("end", "结束", FlowEnums.NodeType.END,
                null, false, List.of(), null);
        FlowNode approval = new FlowNode("approval", "审批", FlowEnums.NodeType.APPROVAL,
                FlowEnums.ApprovalMode.ALL, false, List.of("user-1"), end);
        FlowNode start = new FlowNode("start", "开始", FlowEnums.NodeType.START,
                null, false, List.of(), approval);

        assertThat(start.childNode().childNode().type()).isEqualTo(FlowEnums.NodeType.END);
    }
}
