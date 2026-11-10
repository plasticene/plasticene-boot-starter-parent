package com.plasticene.boot.flow.core.service.task;

import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.parser.FlowReturnPath;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 在入口事务内统一执行实例加锁与任务校验。
 * @author ZFJ
 * @since 2026-11-10
 */
@Service
public class FlowTaskExecution {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    @Lazy
    private FlowRuntimeService flowRuntimeService;

    @Resource
    private FlowDefinitionService flowDefinitionService;

    /** 在当前入口事务内执行待办操作，初读及锁内重读均保留。 */
    public void execute(Long taskId, LoginUser user, BiConsumer<FlowTask, FlowInstance> operation) {
        execute(getExecutableTask(taskId, user), user, operation);
    }

    /** 初读已经完成，供需要在锁前校验审批意见的入口使用。 */
    public void execute(FlowTask initialTask, LoginUser user, BiConsumer<FlowTask, FlowInstance> operation) {
        withTask(initialTask, user, (task, instance) -> {
            operation.accept(task, instance);
            return null;
        });
    }

    public <T> T withTask(Long taskId, LoginUser user, BiFunction<FlowTask, FlowInstance, T> operation) {
        return withTask(getExecutableTask(taskId, user), user, operation);
    }


    public LoginUser getLoginUser() {
        LoginUser loginUser = LoginUserHolder.get();
        if (loginUser == null || loginUser.getId() == null || loginUser.getOrgId() == null) {
            throw new BizException("登录用户信息不存在");
        }
        return loginUser;
    }

    public void requireNodeOperation(FlowNode root, FlowTask task,
                                      Function<FlowNode, Boolean> permission, String operation) {
        FlowNode node = FlowParser.findNodeByKey(root, task.getNodeKey());
        if (node == null) {
            throw new BizException("审批节点不存在");
        }
        if (Boolean.FALSE.equals(permission.apply(node))) {
            throw new BizException("当前审批节点不允许" + operation);
        }
    }

    public FlowNode getReturnModel(FlowInstance instance) {
        FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
        if (definition == null || !Objects.equals(definition.getOrgId(), instance.getOrgId())) {
            throw new BizException("流程发布定义不存在");
        }
        FlowNode root = definition.getModelNode();
        FlowParser.makeParentNode(root);
        return root;
    }

    public FlowTask getExecutableTask(Long taskId, LoginUser loginUser) {
        FlowTask task = flowTaskDAO.selectById(taskId);
        if (task == null
                || !Objects.equals(task.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(task.getAssignee(), loginUser.getId())
                || !Objects.equals(task.getNodeType(), FlowNodeEnum.Type.APPROVE.getCode())) {
            throw new BizException("任务不存在或无权处理");
        }
        if (!FlowReturnPath.isActive(task)
                || !Objects.equals(task.getStatus(), FlowTaskStatusEnum.RUNNING.getCode())
                || !Objects.equals(task.getIsDelete(), CommonConstant.IS_NOT_DEL)) {
            throw new BizException(Boolean.TRUE.equals(task.getTimeoutHandled())
                    ? "任务已超时自动处理，请刷新后查看" : "任务已经处理过，请勿重复处理");
        }
        return task;
    }

    private void validateRunningInstance(FlowInstance instance, LoginUser loginUser, FlowTask task) {
        if (instance == null
                || !Objects.equals(instance.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(instance.getOrgId(), task.getOrgId())
                || !Objects.equals(instance.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())) {
            throw new BizException("流程实例已结束，不能继续审批");
        }
    }

    private <T> T withTask(FlowTask initialTask, LoginUser user, BiFunction<FlowTask, FlowInstance, T> operation) {
        FlowInstance instance = flowRuntimeService.selectInstanceForUpdate(initialTask.getInstanceId());
        FlowTask task = getExecutableTask(initialTask.getId(), user);
        validateRunningInstance(instance, user, task);
        return operation.apply(task, instance);
    }

}
