package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.model.param.FlowFormParam;
import com.plasticene.boot.flow.core.model.query.FlowFormQuery;
import com.plasticene.boot.flow.core.model.vo.FlowFormVO;
import com.plasticene.boot.flow.core.service.FlowFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提供流程表单管理接口。
 * @author ZFJ
 * @since 2026-11-05
 */
@RestController
@RequestMapping("/flow/form")
@Tag(name = "审批流-表单")
public class FlowFormController {
    @Resource
    private FlowFormService flowFormService;

    @Operation(summary = "创建表单")
    @PostMapping
    public ResponseVO<Long> create(@RequestBody @Validated FlowFormParam param) {
        Long id = flowFormService.create(param);
        return ResponseVO.success(id);
    }

    @Operation(summary = "更新表单")
    @PutMapping
    public ResponseVO<Void> update(@RequestBody @Validated FlowFormParam param) {
        flowFormService.update(param);
        return ResponseVO.success();
    }

    @Operation(summary = "删除表单")
    @DeleteMapping
    public ResponseVO<Void> delete(@RequestBody List<Long> ids) {
        flowFormService.delete(ids);
        return ResponseVO.success();
    }

    @Operation(summary = "分页查询表单")
    @GetMapping("/page")
    public ResponseVO<PageResult<FlowFormVO>> page(FlowFormQuery query) {
        PageResult<FlowFormVO> pageResult = flowFormService.page(query);
        return ResponseVO.success(pageResult);
    }

    @Operation(summary = "表单列表")
    @GetMapping("/list")
    public ResponseVO<List<FlowFormVO>> list() {
        List<FlowFormVO> list = flowFormService.listAll();
        return ResponseVO.success(list);
    }

    @Operation(summary = "表单详情")
    @GetMapping("/{id}")
    public ResponseVO<FlowFormVO> detail(@PathVariable("id") Long id) {
        FlowFormVO vo = flowFormService.detail(id);
        return ResponseVO.success(vo);
    }


}
