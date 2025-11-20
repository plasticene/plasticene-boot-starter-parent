package com.plasticene.boot.flow.core.provider;

import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.flow.core.enums.FlowProcessNodeEnum;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public class DefaultFlowTaskAssigneeProvider implements FlowTaskAssigneeProvider {
    @Override
    public List<Long> getAssignees(ProcessNode currentNode) {
        Integer assigneeType = currentNode.getAssigneeType();
        List<Long> assigneeList = currentNode.getAssigneeList();
        if (Objects.equals(assigneeType, FlowProcessNodeEnum.AssigneeType.USER.getCode())) {
            return assigneeList;
        }

        // todo 其他类型需完善
        return Collections.emptyList();
    }

}
