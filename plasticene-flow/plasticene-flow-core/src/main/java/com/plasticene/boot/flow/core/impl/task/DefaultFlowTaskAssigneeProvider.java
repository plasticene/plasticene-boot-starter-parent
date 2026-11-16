package com.plasticene.boot.flow.core.impl.task;

import com.plasticene.boot.flow.core.api.spi.FlowTaskAssigneeProvider;

import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.enums.FlowNodeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public class DefaultFlowTaskAssigneeProvider implements FlowTaskAssigneeProvider {
    @Override
    public List<Long> getAssignees(FlowNode currentNode, Long orgId, Long applicantId) {
        Integer assigneeType = currentNode.getAssigneeType();
        List<Long> assigneeList = currentNode.getAssigneeList();
        if (Objects.equals(assigneeType, FlowNodeEnum.AssigneeType.USER.getCode())) {
            return assigneeList;
        }

        // todo 其他类型需完善
        return new ArrayList<>();
    }

    @Override
    public Long getLeader(Long orgId, Long userId) {
        return 0L;
    }

    @Override
    public Long getDeptLeader(Long orgId, Long userId) {
        return 0L;
    }

}
