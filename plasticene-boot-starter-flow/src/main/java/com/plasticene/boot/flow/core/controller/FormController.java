package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.model.param.FormParam;
import com.plasticene.boot.flow.core.model.query.FormQuery;
import com.plasticene.boot.flow.core.model.vo.FormVO;
import com.plasticene.boot.flow.core.service.FormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ZFJ
 * @since 2026/2/25
 */
@RestController
@RequestMapping("/flow/form")
@Tag(name = "审批流-表单")
public class FormController {
    @Resource
    private FormService formService;

    @Operation(summary = "创建表单")
    @PostMapping
    public ResponseVO<Long> create(@RequestBody @Validated FormParam param) {
        Long id = formService.create(param);
        return ResponseVO.success(id);
    }

    @Operation(summary = "更新表单")
    @PutMapping
    public ResponseVO<Void> update(@RequestBody @Validated FormParam param) {
        formService.update(param);
        return ResponseVO.success();
    }

    @Operation(summary = "删除表单")
    @DeleteMapping
    public ResponseVO<Void> delete(@RequestBody List<Long> ids) {
        formService.delete(ids);
        return ResponseVO.success();
    }

    @Operation(summary = "分页查询表单")
    @GetMapping("/page")
    public ResponseVO<PageResult<FormVO>> page(FormQuery query) {
        PageResult<FormVO> pageResult = formService.page(query);
        return ResponseVO.success(pageResult);
    }

    @Operation(summary = "表单列表")
    @GetMapping("/list")
    public ResponseVO<List<FormVO>> list() {
        List<FormVO> list = formService.listAll();
        return ResponseVO.success(list);
    }

    @Operation(summary = "表单详情")
    @GetMapping("/{id}")
    public ResponseVO<FormVO> detail(@PathVariable("id") Long id) {
        FormVO vo = formService.detail(id);
        return ResponseVO.success(vo);
    }


}
