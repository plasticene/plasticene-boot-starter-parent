package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.flow.core.model.param.CategoryParam;
import com.plasticene.boot.flow.core.model.query.CategoryQuery;
import com.plasticene.boot.flow.core.model.vo.CategoryVO;
import com.plasticene.boot.flow.core.service.CategoryService;
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
public class CategoryController {
    @Resource
    private CategoryService categoryService;


    @Operation(summary = "新增分类")
    @PostMapping
    public ResponseVO<Long> createCategory(@RequestBody CategoryParam param) {
        Long id = categoryService.createCategory(param);
        return ResponseVO.success(id);
    }

    @Operation(summary = "更新分类")
    @PutMapping
    public ResponseVO<Void> updateCategory(@RequestBody @Validated(Update.class) CategoryParam param) {
        categoryService.updateCategory(param);
        return ResponseVO.success();
    }

    @Operation(summary = "分类排序")
    @PostMapping("/sort")
    public ResponseVO<Void> sortCategory(@RequestBody List<Long> ids) {
        categoryService.sortCategory(ids);
        return ResponseVO.success();
    }

    @Operation(summary = "分类列表")
    @GetMapping("/list")
    public ResponseVO<List<CategoryVO>> listCategory() {
        List<CategoryVO> voList = categoryService.listCategory();
        return ResponseVO.success(voList);
    }

    @Operation(summary = "分页查询分类")
    @GetMapping("/page")
    public ResponseVO<PageResult<CategoryVO>> pageCategory(CategoryQuery query) {
        PageResult<CategoryVO> pageResult = categoryService.page(query);
        return ResponseVO.success(pageResult);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping
    public ResponseVO<Void> deleteCategory(@RequestBody List<Long> ids) {
        categoryService.delete(ids);
        return ResponseVO.success();
    }
}
