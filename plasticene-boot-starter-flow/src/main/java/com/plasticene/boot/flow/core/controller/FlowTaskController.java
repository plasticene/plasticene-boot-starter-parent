package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.model.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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
        return ResponseVO.success(flowTaskService.pageMyTodo(query));
    }

    @Operation(summary = "分页查询当前用户已办任务")
    @GetMapping("/my/done/page")
    public ResponseVO<PageResult<FlowTaskPageVO>> pageMyDone(@Validated FlowTaskQuery query) {
        return ResponseVO.success(flowTaskService.pageMyDone(query));
    }

    @Operation(summary = "分页查询抄送给当前用户的任务")
    @GetMapping("/my/copy/page")
    public ResponseVO<PageResult<FlowTaskPageVO>> pageMyCopy(@Validated FlowTaskQuery query) {
        return ResponseVO.success(flowTaskService.pageMyCopy(query));
    }

    @Operation(summary = "审批流程实例任务")
    @PostMapping("/approve")
    public ResponseVO<Void> approveFlowTask(@RequestBody @Validated FlowTaskParam param) {
        flowTaskService.approveTask(param);
        return ResponseVO.success();
    }

    @Operation(summary = "拒绝流程实例任务")
    @PostMapping("/reject")
    public ResponseVO<Void> rejectFlowTask(@RequestBody @Validated FlowTaskParam param) {
        flowTaskService.rejectTask(param);
        return ResponseVO.success();
    }

    @Operation(summary = "转交当前审批待办")
    @PostMapping("/transfer")
    public ResponseVO<Void> transferFlowTask(@RequestBody @Validated FlowTaskTransferParam param) {
        flowTaskService.transferTask(param);
        return ResponseVO.success();
    }

    @Operation(summary = "查询当前待办的退回目标")
    @GetMapping("/{taskId}/return-targets")
    public ResponseVO<List<FlowTaskReturnTargetVO>> listReturnTargets(@PathVariable("taskId") Long taskId) {
        return ResponseVO.success(flowTaskService.listReturnTargets(taskId));
    }

    @Operation(summary = "退回当前审批待办")
    @PostMapping("/return")
    public ResponseVO<Void> returnFlowTask(@RequestBody @Validated FlowTaskReturnParam param) {
        flowTaskService.returnTask(param);
        return ResponseVO.success();
    }

    @Operation(summary = "撤回已同意的审批结果")
    @PostMapping("/{taskId}/withdraw")
    public ResponseVO<Void> withdrawFlowTask(@PathVariable("taskId") Long taskId) {
        flowTaskService.withdrawTask(taskId);
        return ResponseVO.success();
    }
}
