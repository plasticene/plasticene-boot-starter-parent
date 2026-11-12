package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.event.FlowEventAdministration;
import com.plasticene.boot.flow.core.event.FlowEventDelivery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供受宿主运维授权保护的流程事件查询及重投接口。
 * @author ZFJ
 * @since 2026-11-11
 */
@RestController
@RequestMapping("/flow/event")
@Tag(name = "工作流-事件运维")
public class FlowEventController {
    private final FlowEventAdministration administration;

    public FlowEventController(FlowEventAdministration administration) {
        this.administration = administration;
    }

    @GetMapping("/failures")
    @Operation(summary = "查询当前组织的失败流程事件")
    public ResponseVO<List<FlowEventDelivery>> failures(@RequestParam(defaultValue = "50") int limit) {
        return ResponseVO.success(administration.failures(LoginUserHolder.get(), limit));
    }

    @PostMapping("/{eventId}/retry")
    @Operation(summary = "重新投递当前组织已暂停的流程事件")
    public ResponseVO<Void> retry(@PathVariable String eventId) {
        administration.retry(eventId, LoginUserHolder.get());
        return ResponseVO.success();
    }
}
