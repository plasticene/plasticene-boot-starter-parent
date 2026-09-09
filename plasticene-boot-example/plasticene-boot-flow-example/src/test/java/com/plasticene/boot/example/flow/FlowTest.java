package com.plasticene.boot.example.flow;

import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.ProcessConditionRule;
import com.plasticene.boot.flow.core.factory.OperatorFactory;
import com.plasticene.boot.flow.core.operator.Operator;
import com.plasticene.boot.flow.core.parser.FlowParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@SpringBootTest
@Slf4j
public class FlowTest {
    @Resource
    private FlowProcessService flowProcessService;
    @Resource
    private FlowRuntimeServiceTest flowRuntimeServiceTest;
    @Resource
    private ProcessExecutor processExecutor;


    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Test
    public void testParseValidateProcessNode() {
        FlowProcess flowProcess = flowProcessService.getById(3L);
        FlowNode flowNode = flowProcess.getFlowNode();
        Boolean b = FlowParser.validateProcessNode(flowNode);
        System.out.println(b);
    }

    @Test
    public void testFindNodeByKey() {
        FlowProcess flowProcess = flowProcessService.getById(3L);
        FlowNode flowNode = flowProcess.getFlowNode();
        FlowNode node = FlowParser.findNodeByKey(flowNode, "node-012");
        System.out.println(node);
    }

    @Test
    public void testFindNextNode() {
        FlowProcess flowProcess = flowProcessService.getById(3L);
        FlowNode flowNode = flowProcess.getFlowNode();
        FlowNode node = FlowParser.findNodeByKey(flowNode, "node-012");
        FlowNode nextNode = FlowParser.findExecutionNextNode(node);
        System.out.println(nextNode);
    }

    @Test
    public void testGetAllProcessConditionRule() {
        FlowProcess flowProcess = flowProcessService.getById(4L);
        FlowNode flowNode = flowProcess.getFlowNode();
        List<ProcessConditionRule> rules = FlowParser.getAllProcessConditionRules(flowNode);
        System.out.println(rules);

    }


    @Test
    public void testOperatorValidate() {
        Operator operator = OperatorFactory.getOperator(">=");
        operator.validate(1, "123");
    }

    @Test
    public void testOperatorCompare() {
        Operator operator = OperatorFactory.getOperator(">=");
        boolean compare = operator.compare(1, "8", "6");
        System.out.println(compare);
    }


    @Test
    public void testSelectInstanceForUpdate() throws ExecutionException, InterruptedException {
        Future<?> future1 = executorService.submit(() -> flowRuntimeServiceTest.selectInstanceForUpdate(8L));
        Future<?> future2 = executorService.submit(() -> flowRuntimeServiceTest.selectInstanceForUpdate(8L));
        future1.get();
        future2.get();
    }

    @Test
    public void testCalculateRoute() {
        FlowProcess flowProcess = flowProcessService.getById(26L);
        FlowNode flowNode = flowProcess.getFlowNode();
        List<FlowNode> route = processExecutor.calculateRoute(flowNode, Map.of("day", 8));
        System.out.println(route);
    }



}
