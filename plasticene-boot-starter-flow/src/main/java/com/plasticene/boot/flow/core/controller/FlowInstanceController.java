package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.model.param.FlowInstanceParam;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author ZFJ
 * @since 2026/9/4
 */
@RestController
@RequestMapping("/flow/instance")
@Tag(name = "工作流-实例管理")
public class FlowInstanceController {
    @Resource
    private FlowRuntimeService flowRuntimeService;

    @Operation(summary = "基于流程模型发起实例")
    @PostMapping("/start")
    public ResponseVO<Long> startFlowInstance(@RequestBody @Validated FlowInstanceParam param) {
        Long definitionId = param.getDefinitionId();
        Long businessId = param.getBusinessId();
        Map<String, Object> varMap = param.getVarMap();
        long instanceId = flowRuntimeService.startFlowInstanceById(definitionId, businessId, varMap);
        return ResponseVO.success(instanceId);
    }
}
