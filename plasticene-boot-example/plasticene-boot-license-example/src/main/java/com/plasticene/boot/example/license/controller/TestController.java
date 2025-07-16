package com.plasticene.boot.example.license.controller;

import com.plasticene.boot.license.core.anno.License;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ZFJ
 * @date 2025/7/16
 */
@RestController
@RequestMapping("/test")
@Tag(name = "测试license示例")
public class TestController {

    @GetMapping("/anno")
    @Operation(summary = "测试license注解校验")
    @License
    public String testLicense() {
        return "success";
    }
}
