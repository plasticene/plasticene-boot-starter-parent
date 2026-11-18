package com.plasticene.boot.example.flow.purchase;

import com.plasticene.boot.common.pojo.PageQuery;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例业务系统的采购申请 HTTP 接口。
 * @author ZFJ
 * @since 2026-11-18
 */
@RestController
@RequestMapping("/flow/demo/purchase")
@Tag(name = "业务审批示例-采购申请")
public class PurchaseApplyController {
    private final PurchaseApplyService service;

    public PurchaseApplyController(PurchaseApplyService service) {
        this.service = service;
    }

    /** 保存采购申请并启动审批，返回关联的流程实例。 */
    @PostMapping("/submit")
    @Operation(summary = "提交采购申请")
    public ResponseVO<PurchaseApplyVO> submit(@RequestBody @Valid PurchaseApplyParam param) {
        return ResponseVO.success(service.submit(param, LoginUserHolder.get()));
    }

    /** 分页查询登录用户本人提交的采购申请。 */
    @GetMapping("/page")
    @Operation(summary = "分页查询我的采购申请")
    public ResponseVO<PageResult<PurchaseApplyVO>> page(@Valid PageQuery query) {
        return ResponseVO.success(service.page(query, LoginUserHolder.get()));
    }

    /** 查询已授权流程实例所关联的采购申请。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询采购申请详情")
    public ResponseVO<PurchaseApplyVO> detail(@PathVariable("id") Long id) {
        return ResponseVO.success(service.detail(id, LoginUserHolder.get()));
    }
}
