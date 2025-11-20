package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.model.param.CategoryParam;
import com.plasticene.boot.flow.core.model.param.FlowInstanceParam;
import com.plasticene.boot.flow.core.model.param.FlowProcessParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.vo.CategoryVO;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.web.core.validator.Insert;
import com.plasticene.boot.web.core.validator.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @Resource
    private CategoryService categoryService;


    @Operation(summary = "创建流程模型")
    @PostMapping("/process")
    public ResponseVO<Long> createFlowProcess(@RequestBody @Validated(Insert.class) FlowProcessParam param) {
        Long id = flowProcessService.createFlowProcess(param);
        return ResponseVO.success(id);
    }

    @Operation(summary = "修改流程模型")
    @PutMapping("/process")
    public ResponseVO<Void> updateFlowProcess(@RequestBody @Validated(Update.class) FlowProcessParam param) {
        flowProcessService.updateFlowProcess(param);
        return ResponseVO.success();
    }

    @Operation(summary = "基于流程id发布")
    @PostMapping("/process/release/{processId}")
    public ResponseVO<Void> releaseFlowProcess(@PathVariable("processId") Long processId) {
        flowProcessService.releaseFlowProcess(processId);
        return ResponseVO.success();
    }

    @Operation(summary = "基于流程配置直接发布")
    @PostMapping("/process/release")
    public ResponseVO<Void> releaseFlowProcess(@RequestBody FlowProcessParam param) {
        flowProcessService.releaseFlowProcess(param);
        return ResponseVO.success();
    }

    @Operation(summary = "获取流程列表(基于分组分类)")
    @GetMapping("/process")
    public ResponseVO<List<CategoryVO>> listFlowProcess(@RequestParam(name = "processName", required = false) String processName) {
        List<CategoryVO> voList = flowProcessService.listFlowProcess(processName);
        return ResponseVO.success(voList);
    }

    @Operation(summary = "基于流程模型发起实例")
    @PostMapping("/instance/start")
    public ResponseVO<Long> startFlowInstance(@RequestBody @Validated FlowInstanceParam param) {
        Long processId = param.getProcessId();
        Long businessId = param.getBusinessId();
        Map<String, Object> varMap = param.getVarMap();
        long instanceId = flowRuntimeService.startFlowInstanceById(processId, businessId, varMap);
        return ResponseVO.success(instanceId);
    }

    @Operation(summary = "审批流程实例任务")
    @PostMapping("/task/approve")
    public ResponseVO<Void> approveFlowTask(@RequestBody @Validated FlowTaskParam param) {
        flowTaskService.approveTask(param);
        return ResponseVO.success();
    }

    @Operation(summary = "拒绝流程实例任务")
    @PostMapping("/task/reject")
    public ResponseVO<Void> rejectFlowTask(@RequestBody @Validated FlowTaskParam param) {
        flowTaskService.rejectTask(param);
        return ResponseVO.success();
    }

    @Operation(summary = "新增分组")
    @PostMapping("/category")
    public ResponseVO<Long> createCategory(@RequestBody CategoryParam param) {
        Long id = categoryService.createCategory(param);
        return ResponseVO.success(id);
    }

    @Operation(summary = "更新分组")
    @PutMapping("/category")
    public ResponseVO<Void> updateCategory(@RequestBody @Validated(Update.class) CategoryParam param) {
        categoryService.updateCategory(param);
        return ResponseVO.success();
    }

    @Operation(summary = "分组排序")
    @PostMapping("/category/sort")
    public ResponseVO<Void> sortCategory(CategoryParam param) {
        List<Long> ids = param.getIds();
        categoryService.sortCategory(ids);
        return ResponseVO.success();
    }

    @Operation(summary = "分组列表")
    @GetMapping("/category")
    public ResponseVO<List<CategoryVO>> listCategory() {
        List<CategoryVO> voList = categoryService.listCategory();
        return ResponseVO.success(voList);
    }
}
