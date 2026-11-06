package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.model.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.model.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.model.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.web.core.validator.Insert;
import com.plasticene.boot.web.core.validator.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ZFJ
 * @since 2026/2/9
 */
@RestController
@RequestMapping("/flow/category")
@Tag(name = "审批流-分类管理")
public class FlowCategoryController {
    @Resource
    private FlowCategoryService flowCategoryService;


    @Operation(summary = "新增分类")
    @PostMapping
    public ResponseVO<Long> createCategory(@RequestBody @Validated(Insert.class) FlowCategoryParam param) {
        Long id = flowCategoryService.createCategory(param);
        return ResponseVO.success(id);
    }

    @Operation(summary = "更新分类")
    @PutMapping
    public ResponseVO<Void> updateCategory(@RequestBody @Validated(Update.class) FlowCategoryParam param) {
        flowCategoryService.updateCategory(param);
        return ResponseVO.success();
    }

    @Operation(summary = "分类排序")
    @PostMapping("/sort")
    public ResponseVO<Void> sortCategory(@RequestBody List<Long> ids) {
        flowCategoryService.sortCategory(ids);
        return ResponseVO.success();
    }

    @Operation(summary = "分类列表")
    @GetMapping("/list")
    public ResponseVO<List<FlowCategoryVO>> listCategory() {
        List<FlowCategoryVO> voList = flowCategoryService.listCategory();
        return ResponseVO.success(voList);
    }

    @Operation(summary = "分页查询分类")
    @GetMapping("/page")
    public ResponseVO<PageResult<FlowCategoryVO>> pageCategory(FlowCategoryQuery query) {
        PageResult<FlowCategoryVO> pageResult = flowCategoryService.page(query);
        return ResponseVO.success(pageResult);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping
    public ResponseVO<Void> deleteCategory(@RequestBody List<Long> ids) {
        flowCategoryService.delete(ids);
        return ResponseVO.success();
    }
}
