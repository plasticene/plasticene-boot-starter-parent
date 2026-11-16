package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;
import com.plasticene.boot.flow.core.api.service.*;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.validation.group.*;
import jakarta.validation.groups.Default;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;

/**
 * 实现FlowTaskOperations的统一授权与操作编排。
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
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void approve(FlowTaskParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_APPROVE, access.task(param.getTaskId()));
        taskService.approveTask(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void reject(FlowTaskParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_REJECT, access.task(param.getTaskId()));
        taskService.rejectTask(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void saveForm(FlowTaskFormParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_SAVE_FORM, access.task(param.getTaskId()));
        taskService.saveForm(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void addSign(FlowTaskAddSignParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_ADD_SIGN, access.task(param.getTaskId()));
        taskService.addSign(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void transfer(FlowTaskTransferParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_TRANSFER, access.task(param.getTaskId()));
        taskService.transferTask(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void returnTask(FlowTaskReturnParam param, LoginUser actor) {
        FlowActors.require(actor);
        access.validate(param, Default.class);
        access.check(actor, FlowAction.TASK_RETURN, access.task(param.getTaskId()));
        taskService.returnTask(param, actor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
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
