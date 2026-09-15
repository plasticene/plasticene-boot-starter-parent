package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.model.param.FlowInstanceParam;
import com.plasticene.boot.flow.core.model.param.FlowRouteParam;
import com.plasticene.boot.flow.core.model.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.model.vo.FlowRouteNodeVO;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
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

    @Operation(summary = "计算流程预计审批路径")
    @PostMapping("/route")
    public ResponseVO<List<FlowRouteNodeVO>> calculateRoute(@RequestBody @Validated FlowRouteParam param) {
        return ResponseVO.success(flowRuntimeService.calculateRoute(param.getDefinitionId(), param.getVarMap()));
    }

    @Operation(summary = "分页查询流程实例")
    @GetMapping("/page")
    public ResponseVO<PageResult<FlowInstanceVO>> page(@Validated FlowInstanceQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        query.setOrgId(loginUser.getOrgId());
        return ResponseVO.success(flowRuntimeService.page(query));
    }

    @Operation(summary = "分页查询当前用户发起的流程实例")
    @GetMapping("/my/page")
    public ResponseVO<PageResult<FlowInstanceVO>> pageMyApplications(@Validated FlowInstanceQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        query.setUserId(loginUser.getId());
        query.setOrgId(loginUser.getOrgId());
        return ResponseVO.success(flowRuntimeService.page(query));
    }

    @Operation(summary = "查询流程实例详情")
    @GetMapping("/{instanceId}")
    public ResponseVO<FlowInstanceDetailVO> getInstanceDetail(@PathVariable("instanceId") Long instanceId) {
        return ResponseVO.success(flowRuntimeService.getInstanceDetail(instanceId));
    }

    @Operation(summary = "取消当前用户发起的流程实例")
    @PostMapping("/{instanceId}/cancel")
    public ResponseVO<Void> cancelInstance(@PathVariable("instanceId") Long instanceId) {
        flowRuntimeService.cancelInstance(instanceId);
        return ResponseVO.success();
    }
}
