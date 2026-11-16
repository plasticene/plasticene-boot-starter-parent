package com.plasticene.boot.flow.rest.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.api.query.FlowDashboardQuery;
import com.plasticene.boot.flow.core.api.vo.FlowDashboardVO;
import com.plasticene.boot.flow.core.api.service.FlowQueries;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程数据总览控制器
 *
 * @author ZFJ
 * @since 2026-09-17
 */
@RestController
@RequestMapping("/flow/dashboard")
@Tag(name = "工作流-数据总览")
public class FlowDashboardController {

    @Resource
    private FlowQueries flowDashboardService;

    @Operation(summary = "查询组织级流程数据总览")
    @GetMapping("/overview")
    public ResponseVO<FlowDashboardVO> overview(@Validated FlowDashboardQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        query.setOrgId(loginUser.getOrgId());
        return ResponseVO.success(flowDashboardService.dashboard(query, LoginUserHolder.get()));
    }
}
