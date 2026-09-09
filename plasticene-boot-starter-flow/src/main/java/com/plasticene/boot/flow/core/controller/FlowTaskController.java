package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
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
}
