package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.param.FlowProcessParam;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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


}
