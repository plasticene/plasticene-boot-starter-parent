package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.param.FlowInstanceParam;
import com.plasticene.boot.flow.core.param.FlowProcessParam;
import com.plasticene.boot.flow.core.param.FlowTaskParam;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.flow.core.vo.FlowProcessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@RestController
@RequestMapping("/flow")
@Tag(name = "工作流管理")
public class FlowController {

    @Resource
    private FlowProcessService flowProcessService;
    @Resource
    private FlowRuntimeService flowRuntimeService;
    @Resource
    private FlowTaskService flowTaskService;


    @PostMapping("/process")
    @Operation(summary = "创建流程模型")
    public ResponseVO<Long> createFlowProcess(@RequestBody @Validated FlowProcessParam param) {
        Long id = flowProcessService.createFlowProcess(param);
        return ResponseVO.success(id);
    }

    @PutMapping("/process")
    @Operation(summary = "修改流程模型")
    public ResponseVO<Void> updateFlowProcess(@RequestBody FlowProcessParam param) {
        flowProcessService.updateFlowProcess(param);
        return ResponseVO.success();
    }

    @GetMapping("/process")
    @Operation(summary = "获取流程列表")
    public ResponseVO<List<FlowProcessVO>> listFlowProcess() {

        return ResponseVO.success(null);
    }

    @PostMapping("/instance/start")
    @Operation(summary = "基于流程模型发起实例")
    public ResponseVO<Void> startFlowInstance(@RequestBody FlowInstanceParam param) {
        flowRuntimeService.startFlowInstanceById(param.getProcessId());
        return ResponseVO.success();
    }

    @PostMapping("/task/approve")
    @Operation(summary = "审批流程实例任务")
    public ResponseVO<Void> approveFlowTask(@RequestBody @Validated FlowTaskParam param) {
        flowTaskService.approveTask(param);
        return ResponseVO.success();
    }

    @PostMapping("/task/reject")
    @Operation(summary = "拒绝流程实例任务")
    public ResponseVO<Void> rejectFlowTask(@RequestBody @Validated FlowTaskParam param) {
        flowTaskService.rejectTask(param);
        return ResponseVO.success();
    }
}
