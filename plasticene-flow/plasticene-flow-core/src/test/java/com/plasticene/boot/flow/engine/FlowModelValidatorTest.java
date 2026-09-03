package com.plasticene.boot.flow.engine;

import com.plasticene.boot.flow.core.FlowException;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 流程模型校验器单元测试。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
class FlowModelValidatorTest {

    private final FlowModelValidator validator = new FlowModelValidator();

    @Test
    void rejectsAModelWithoutApprovalNode() {
        FlowNode end = new FlowNode("end", "End", FlowEnums.NodeType.END, null, false, List.of(), null);
        FlowNode start = new FlowNode("start", "Start", FlowEnums.NodeType.START, null, false, List.of(), end);

        assertThatThrownBy(() -> validator.validate(start))
                .isInstanceOf(FlowException.class)
                .hasMessageContaining("approval node");
    }
}
