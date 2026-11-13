package com.plasticene.boot.flow.web.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.api.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ZFJ
 * @since 2026/9/4
 */
@RestController
@RequestMapping("/flow/task")
@Tag(name = "工作流-任务管理")
public class FlowTaskController {
    @Resource
    private FlowTaskService flowTaskService;

    @Operation(summary = "分页查询当前用户待办任务")
    @GetMapping("/my/todo/page")
    public ResponseVO<PageResult<FlowTaskPageVO>> pageMyTodo(@Validated FlowTaskQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowTaskService.pageMyTodo(query, loginUser));
    }

    @Operation(summary = "分页查询当前用户已办任务")
    @GetMapping("/my/done/page")
    public ResponseVO<PageResult<FlowTaskPageVO>> pageMyDone(@Validated FlowTaskQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowTaskService.pageMyDone(query, loginUser));
    }

    @Operation(summary = "分页查询抄送给当前用户的任务")
    @GetMapping("/my/copy/page")
    public ResponseVO<PageResult<FlowTaskPageVO>> pageMyCopy(@Validated FlowTaskQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowTaskService.pageMyCopy(query, loginUser));
    }

    @Operation(summary = "保存当前审批任务的表单修改")
    @PostMapping("/form")
    public ResponseVO<Void> saveForm(@RequestBody @Validated FlowTaskFormParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowTaskService.saveForm(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "审批流程实例任务")
    @PostMapping("/approve")
    public ResponseVO<Void> approveFlowTask(@RequestBody @Validated FlowTaskParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowTaskService.approveTask(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "拒绝流程实例任务")
    @PostMapping("/reject")
    public ResponseVO<Void> rejectFlowTask(@RequestBody @Validated FlowTaskParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowTaskService.rejectTask(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "加签当前审批待办")
    @PostMapping("/add-sign")
    public ResponseVO<Void> addSign(@RequestBody @Validated FlowTaskAddSignParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowTaskService.addSign(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "转交当前审批待办")
    @PostMapping("/transfer")
    public ResponseVO<Void> transferFlowTask(@RequestBody @Validated FlowTaskTransferParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowTaskService.transferTask(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "查询当前待办的退回目标")
    @GetMapping("/{taskId}/return-targets")
    public ResponseVO<List<FlowTaskReturnTargetVO>> listReturnTargets(@PathVariable("taskId") Long taskId) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowTaskService.listReturnTargets(taskId, loginUser));
    }

    @Operation(summary = "退回当前审批待办")
    @PostMapping("/return")
    public ResponseVO<Void> returnFlowTask(@RequestBody @Validated FlowTaskReturnParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowTaskService.returnTask(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "撤回已同意的审批结果")
    @PostMapping("/{taskId}/withdraw")
    public ResponseVO<Void> withdrawFlowTask(@PathVariable("taskId") Long taskId) {
        LoginUser loginUser = LoginUserHolder.get();
        flowTaskService.withdrawTask(taskId, loginUser);
        return ResponseVO.success();
    }
}
