package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.flow.core.param.FlowProcessParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@RestController
@RequestMapping("/flow")
@Tag(name = "工作流管理")
public class FlowController {


    @PostMapping("/process")
    @Operation(summary = "创建流程模型")
    public Long createFlowProcess(@RequestBody @Validated FlowProcessParam param) {

    }
}
