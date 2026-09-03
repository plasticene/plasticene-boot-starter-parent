package com.plasticene.boot.example.flow;

import com.plasticene.boot.flow.core.FlowEngine;
import com.plasticene.boot.flow.core.command.FlowCommands;
import com.plasticene.boot.flow.core.command.FlowQueries;
import com.plasticene.boot.flow.core.command.FlowResults;
import com.plasticene.boot.flow.core.model.FlowActor;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowNode;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 演示通过 FlowEngine 创建、发布和启动审批流。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@Component
@ConditionalOnProperty(prefix = "demo.flow", name = "run", havingValue = "true")
public class FlowDemoRunner implements ApplicationRunner {

    private final FlowEngine flowEngine;

    public FlowDemoRunner(FlowEngine flowEngine) {
        this.flowEngine = flowEngine;
    }

    @Override
    public void run(ApplicationArguments args) {
        FlowActor actor = new FlowActor("1", "1001", Set.of("employee"));
        FlowNode end = new FlowNode("end", "结束", FlowEnums.NodeType.END,
                null, false, List.of(), null);
        FlowNode approval = new FlowNode("manager-approval", "主管审批", FlowEnums.NodeType.APPROVAL,
                FlowEnums.ApprovalMode.ANY, true, List.of("2001", "2002"), end);
        FlowNode start = new FlowNode("start", "开始", FlowEnums.NodeType.START,
                null, false, List.of(), approval);

        FlowResults.ModelResult model = flowEngine.saveModel(new FlowCommands.SaveModel(
                requestId(), actor, null, "leave", "请假审批", null, start));
        flowEngine.publishModel(new FlowCommands.PublishModel(
                requestId(), actor, model.model().id()));
        FlowResults.InstanceResult instance = flowEngine.startFlow(new FlowCommands.StartFlow(
                requestId(), actor, "leave", "2026001",
                Map.of("days", 2), Map.of("reason", "年假")));
        flowEngine.getInstanceDetails(new FlowQueries.InstanceDetails(actor, instance.instance().id()));
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }
}
