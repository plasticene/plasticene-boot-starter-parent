package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.api.service.FlowTaskApi;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import jakarta.annotation.Resource;
import jakarta.validation.groups.Default;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 实现 {@link FlowTaskApi}，负责审批任务操作的授权与用例编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowTaskApiImpl implements FlowTaskApi {
    @Resource
    private FlowAccess access;
    @Resource
    private FlowTaskService taskService;

    @Override
    public void approve(FlowTaskParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_APPROVE, access.task(param.getTaskId()));
        taskService.approveTask(param, actor);
    }

    @Override
    public void reject(FlowTaskParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_REJECT, access.task(param.getTaskId()));
        taskService.rejectTask(param, actor);
    }

    @Override
    public void saveForm(FlowTaskFormParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_SAVE_FORM, access.task(param.getTaskId()));
        taskService.saveForm(param, actor);
    }

    @Override
    public void addSign(FlowTaskAddSignParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_ADD_SIGN, access.task(param.getTaskId()));
        taskService.addSign(param, actor);
    }

    @Override
    public void transfer(FlowTaskTransferParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_TRANSFER, access.task(param.getTaskId()));
        taskService.transferTask(param, actor);
    }

    @Override
    public void returnTask(FlowTaskReturnParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_RETURN, access.task(param.getTaskId()));
        taskService.returnTask(param, actor);
    }

    @Override
    public void withdraw(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.TASK_WITHDRAW, access.task(id));
        taskService.withdrawTask(id, actor);
    }

    @Override
    public List<FlowTaskReturnTargetVO> listReturnTargets(Long id, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.TASK_RETURN, access.task(id));
        return taskService.listReturnTargets(id, actor);
    }
}
