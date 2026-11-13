package com.plasticene.boot.flow.web.controller;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.impl.model.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.impl.model.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.impl.model.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryService;
import com.plasticene.boot.flow.core.impl.validation.group.Insert;
import com.plasticene.boot.flow.core.impl.validation.group.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
        LoginUser loginUser = LoginUserHolder.get();
        Long id = flowCategoryService.createCategory(param, loginUser);
        return ResponseVO.success(id);
    }

    @Operation(summary = "更新分类")
    @PutMapping
    public ResponseVO<Void> updateCategory(@RequestBody @Validated(Update.class) FlowCategoryParam param) {
        LoginUser loginUser = LoginUserHolder.get();
        flowCategoryService.updateCategory(param, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "分类排序")
    @PostMapping("/sort")
    public ResponseVO<Void> sortCategory(@RequestBody List<Long> ids) {
        LoginUser loginUser = LoginUserHolder.get();
        flowCategoryService.sortCategory(ids, loginUser);
        return ResponseVO.success();
    }

    @Operation(summary = "分类列表")
    @GetMapping("/list")
    public ResponseVO<List<FlowCategoryVO>> listCategory() {
        LoginUser loginUser = LoginUserHolder.get();
        List<FlowCategoryVO> voList = flowCategoryService.listCategory(loginUser);
        return ResponseVO.success(voList);
    }

    @Operation(summary = "分页查询分类")
    @GetMapping("/page")
    public ResponseVO<PageResult<FlowCategoryVO>> pageCategory(FlowCategoryQuery query) {
        LoginUser loginUser = LoginUserHolder.get();
        PageResult<FlowCategoryVO> pageResult = flowCategoryService.page(query, loginUser);
        return ResponseVO.success(pageResult);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping
    public ResponseVO<Void> deleteCategory(@RequestBody List<Long> ids) {
        LoginUser loginUser = LoginUserHolder.get();
        flowCategoryService.delete(ids, loginUser);
        return ResponseVO.success();
    }
}
