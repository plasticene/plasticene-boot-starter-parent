package com.plasticene.boot.flow.rest.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.api.param.FlowFormStartParam;
import com.plasticene.boot.flow.core.api.param.FlowRoutePreviewParam;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowRouteNodeVO;
import com.plasticene.boot.flow.core.api.service.FlowInstanceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ZFJ
 * @since 2026/9/4
 */
@RestController
@RequestMapping("/flow/instance")
@Tag(name = "工作流-实例管理")
public class FlowInstanceController {
    @Resource
    private FlowInstanceApi flowRuntimeService;
    @Resource
    private com.plasticene.boot.flow.core.api.service.FlowQueryApi flowQueryApi;

    @Operation(summary = "基于流程模型发起实例")
    @PostMapping("/start")
    public ResponseVO<Long> startFlowInstance(@RequestBody @Validated FlowFormStartParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        Long definitionId = param.getDefinitionId();
        Map<String, Object> varMap = param.getVarMap();
        Long instanceId = flowRuntimeService.startForm(param, loginUser);
        return ResponseVO.success(instanceId);
    }

    @Operation(summary = "计算流程预计审批路径")
    @PostMapping("/route")
    public ResponseVO<List<FlowRouteNodeVO>> calculateRoute(@RequestBody @Validated FlowRoutePreviewParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowRuntimeService.previewRoute(param, loginUser));
    }

    @Operation(summary = "分页查询流程实例")
    @GetMapping("/page")
    public ResponseVO<PageResult<FlowInstanceVO>> page(@Validated FlowInstanceQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        query.setOrgId(loginUser.getOrgId());
        return ResponseVO.success(flowQueryApi.pageInstances(query, loginUser));
    }

    @Operation(summary = "统计流程实例状态数量")
    @GetMapping("/statistics")
    public ResponseVO<FlowInstanceStatisticsVO> statistics(@Validated FlowInstanceQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        query.setOrgId(loginUser.getOrgId());
        return ResponseVO.success(flowQueryApi.instanceStatistics(query, loginUser));
    }

    @Operation(summary = "分页查询当前用户发起的流程实例")
    @GetMapping("/my/page")
    public ResponseVO<PageResult<FlowInstanceVO>> pageMyApplications(@Validated FlowInstanceQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        query.setUserId(loginUser.getId());
        query.setOrgId(loginUser.getOrgId());
        return ResponseVO.success(flowQueryApi.pageMyInstances(query, loginUser));
    }

    @Operation(summary = "查询流程实例详情")
    @GetMapping("/{instanceId}")
    public ResponseVO<FlowInstanceDetailVO> getInstanceDetail(@PathVariable("instanceId") Long instanceId,
            @RequestParam(value = "taskId", required = false) Long taskId) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowQueryApi.getInstance(instanceId, taskId, loginUser));
    }

    @Operation(summary = "取消当前用户发起的流程实例")
    @PostMapping("/{instanceId}/cancel")
    public ResponseVO<Void> cancelInstance(@PathVariable("instanceId") Long instanceId) {
        LoginUser loginUser = LoginUserHolder.get();
        flowRuntimeService.cancel(instanceId, loginUser);
        return ResponseVO.success();
    }
}
