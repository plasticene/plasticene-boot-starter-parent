package com.plasticene.boot.example.flow;

import com.plasticene.boot.flow.core.model.dto.ProcessConditionRule;
import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.factory.OperatorFactory;
import com.plasticene.boot.flow.core.operator.Operator;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
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


    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Test
    public void testParseValidateProcessNode() {
        FlowProcess flowProcess = flowProcessService.getById(3L);
        ProcessNode processNode = flowProcess.getProcessNode();
        Boolean b = FlowParser.validateProcessNode(processNode);
        System.out.println(b);
    }

    @Test
    public void testFindNodeByKey() {
        FlowProcess flowProcess = flowProcessService.getById(3L);
        ProcessNode processNode = flowProcess.getProcessNode();
        ProcessNode node = FlowParser.findNodeByKey(processNode, "node-012");
        System.out.println(node);
    }

    @Test
    public void testFindNextNode() {
        FlowProcess flowProcess = flowProcessService.getById(3L);
        ProcessNode processNode = flowProcess.getProcessNode();
        ProcessNode node = FlowParser.findNodeByKey(processNode, "node-012");
        ProcessNode nextNode = FlowParser.findExecutionNextNode(node);
        System.out.println(nextNode);
    }

    @Test
    public void testGetAllProcessConditionRule() {
        FlowProcess flowProcess = flowProcessService.getById(4L);
        ProcessNode processNode = flowProcess.getProcessNode();
        List<ProcessConditionRule> rules = FlowParser.getAllProcessConditionRules(processNode);
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



}
