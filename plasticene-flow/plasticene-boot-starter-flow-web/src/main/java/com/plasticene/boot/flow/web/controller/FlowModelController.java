package com.plasticene.boot.flow.web.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.impl.model.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.impl.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.impl.model.query.FlowModelQuery;
import com.plasticene.boot.flow.core.api.vo.FlowFormChangeRecordVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowRouteNodeVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskAddSignVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskVO;
import com.plasticene.boot.flow.core.api.vo.FlowTimeoutNoticeVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowDashboardVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowDefinitionVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowFormVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowModelStatisticsVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowModelVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowStartCatalogVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowStartableModelVO;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.service.model.FlowModelService;
import com.plasticene.boot.flow.core.impl.validation.group.Insert;
import com.plasticene.boot.flow.core.impl.validation.group.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 *
 * <p> 工作流-流程模型 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */
@RestController
@RequestMapping("/flow/model")
@Tag(name = "工作流-流程模型管理")
public class FlowModelController {

    @Resource
    private FlowModelService flowModelService;
    @Resource
    private FlowDefinitionService flowDefinitionService;

    @Operation(summary = "创建工作流-流程模型")
    @PostMapping
    public ResponseVO<Long> create(@RequestBody @Validated(Insert.class) FlowModelParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        Long id = flowModelService.create(param, loginUser);
        return ResponseVO.success(id);
    }

    @Operation(summary = "修改工作流-流程模型")
    @PutMapping
    public ResponseVO<Void> update(@RequestBody @Validated(Update.class) FlowModelParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowModelService.update(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "批量删除工作流-流程模型")
    @DeleteMapping
    public ResponseVO<Void> delete(@RequestBody List<Long> idList) {
        LoginUser loginUser = LoginUserHolder.get();
        flowModelService.delete(idList, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "分页查询工作流-流程模型")
    @GetMapping("/page")
    public ResponseVO<PageResult<FlowModelVO>> page(@Validated FlowModelQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        PageResult<FlowModelVO> pageResult = flowModelService.page(query, loginUser);
        return ResponseVO.success(pageResult);
    }

    @Operation(summary = "获取工作流-流程模型详情")
    @GetMapping("/{id}")
    public ResponseVO<FlowModelVO> detail(@PathVariable("id") Long id) {
        LoginUser loginUser = LoginUserHolder.get();
        FlowModelVO vo = flowModelService.detail(id, loginUser);
        return ResponseVO.success(vo);
    }

    @Operation(summary = "统计工作流-流程模型")
    @GetMapping("/statistics")
    public ResponseVO<FlowModelStatisticsVO> statistics(FlowModelQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        FlowModelStatisticsVO vo = flowModelService.statistics(query, loginUser);
        return ResponseVO.success(vo);
    }

    @Operation(summary = "获取当前用户可发起的流程目录")
    @GetMapping("/start/catalog")
    public ResponseVO<FlowStartCatalogVO> getStartableCatalog() {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowDefinitionService.getStartableCatalog(loginUser));
    }

    @Operation(summary = "获取工作流-流程定义详情")
    @GetMapping("/definition/{definitionId}")
    public ResponseVO<FlowDefinitionVO> getFlowDefinition(@PathVariable("definitionId") Long definitionId) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowDefinitionService.getFlowDefinition(definitionId, loginUser));
    }

    @Operation(summary = "查询流程模型发布历史")
    @GetMapping("/{id}/versions")
    public ResponseVO<List<FlowDefinitionVO>> listVersions(@PathVariable("id") Long id) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowDefinitionService.listModelVersions(id, loginUser));
    }

    @Operation(summary = "查看流程模型发布快照")
    @GetMapping("/{id}/versions/{definitionId}")
    public ResponseVO<FlowDefinitionVO> getVersion(@PathVariable("id") Long id,
                                                        @PathVariable("definitionId") Long definitionId) {
        LoginUser loginUser = LoginUserHolder.get();
        return ResponseVO.success(flowDefinitionService.getModelVersion(id, definitionId, loginUser));
    }

    @Operation(summary = "开关工作流-流程模型")
    @PutMapping("/enable")
    public ResponseVO<Void> enable(@RequestBody @Validated FlowModelEnableParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowModelService.enable(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "发布工作流-流程模型")
    @PostMapping("/publish/{id}")
    public ResponseVO<Void> publish(@PathVariable("id") Long id) {
        LoginUser loginUser = LoginUserHolder.get();
        flowModelService.publish(id, loginUser);
        return ResponseVO.success();
    }
}
