package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.flow.core.model.vo.FlowModelStatisticsVO;
import com.plasticene.boot.flow.core.service.FlowModelService;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.model.query.FlowModelQuery;
import com.plasticene.boot.flow.core.model.vo.FlowModelVO;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.web.core.validator.Insert;
import com.plasticene.boot.web.core.validator.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;


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

    @Operation(summary = "创建工作流-流程模型")
    @PostMapping
    public ResponseVO<Long> create(@RequestBody @Validated(Insert.class) FlowModelParam param) {
        Long id = flowModelService.create(param);
        return ResponseVO.success(id);
    }

    @Operation(summary = "修改工作流-流程模型")
    @PutMapping
    public ResponseVO<Void> update(@RequestBody @Validated(Update.class) FlowModelParam param) {
        flowModelService.update(param);
        return ResponseVO.success();
    }

    @Operation(summary = "批量删除工作流-流程模型")
    @DeleteMapping
    public ResponseVO<Void> delete(@RequestBody List<Long> idList) {
        flowModelService.delete(idList);
        return ResponseVO.success();
    }

    @Operation(summary = "分页查询工作流-流程模型")
    @GetMapping("/page")
    public ResponseVO<PageResult<FlowModelVO>> page(@Validated FlowModelQuery query) {
        PageResult<FlowModelVO> pageResult = flowModelService.page(query);
        return ResponseVO.success(pageResult);
    }

    @Operation(summary = "获取工作流-流程模型详情")
    @GetMapping("/{id}")
    public ResponseVO<FlowModelVO> detail(@PathVariable("id") Long id) {
        FlowModelVO vo = flowModelService.detail(id);
        return ResponseVO.success(vo);
    }

    @Operation(summary = "统计工作流-流程模型")
    @GetMapping("/statistics")
    public ResponseVO<FlowModelStatisticsVO> statistics(FlowModelQuery query) {
        FlowModelStatisticsVO vo = flowModelService.statistics(query);
        return ResponseVO.success(vo);
    }

    @Operation(summary = "开关工作流-流程模型")
    @PutMapping("/enable")
    public ResponseVO<Void> enable(@RequestBody FlowModelParam param) {
        flowModelService.enable(param);
        return ResponseVO.success();
    }

    @Operation(summary = "发布工作流-流程模型")
    @PostMapping("/publish/{id}")
    public ResponseVO<Void> publish(@PathVariable("id") Long id) {
        flowModelService.publish(id);
        return ResponseVO.success();
    }
}