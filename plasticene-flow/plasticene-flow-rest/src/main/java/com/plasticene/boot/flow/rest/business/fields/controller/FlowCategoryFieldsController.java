package com.plasticene.boot.flow.rest.business.fields.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.api.service.FlowBusinessFieldsApi;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理指定流程分类的审批业务字段配置。
 * @author ZFJ
 * @since 2026-11-20
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/flow/category/{categoryId}/fields")
public class FlowCategoryFieldsController {

    private final FlowBusinessFieldsApi fieldsApi;

    @GetMapping
    @Operation(summary = "读取分类审批字段配置")
    public ResponseVO<FlowBusinessFieldsVO.Configuration> get(@PathVariable("categoryId") Long categoryId) {
        return ResponseVO.success(fieldsApi.getFields(categoryId, LoginUserHolder.get()));
    }

    @PutMapping
    @Operation(summary = "保存分类审批字段配置")
    public ResponseVO<Void> save(@PathVariable("categoryId") Long categoryId,
                                 @RequestBody List<FlowBusinessFieldsVO.Field> fields) {
        fieldsApi.saveFields(categoryId, fields, LoginUserHolder.get());
        return ResponseVO.success();
    }
}
