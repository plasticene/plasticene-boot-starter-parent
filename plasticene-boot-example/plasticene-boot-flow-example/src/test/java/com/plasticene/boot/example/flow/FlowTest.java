package com.plasticene.boot.example.flow;

import com.plasticene.boot.flow.core.dto.ProcessNode;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@SpringBootTest
public class FlowTest {
    @Resource
    private FlowProcessService flowProcessService;

    @Test
    public void testParseProcessNode() {
        FlowProcess flowProcess = flowProcessService.getById(2L);
        String model = flowProcess.getModel();
        ProcessNode processNode = FlowParser.parseProcessNode(model);
        System.out.println(processNode);

    }
}
