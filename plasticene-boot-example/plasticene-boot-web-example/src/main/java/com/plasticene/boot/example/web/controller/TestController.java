package com.plasticene.boot.example.web.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.example.web.param.UserParam;
import com.plasticene.boot.example.web.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/21
 */
@Tag(name = "测试示例管理")
@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @Operation(summary = "获取消息")
    @GetMapping("/msg")
    public ResponseVO<String> getMessage() {
        return ResponseVO.success("哈哈😄");
    }

    @Operation(summary = "保存用户信息并返回")
    @PostMapping("/user")
    public ResponseVO<UserVO> addUser(@RequestBody UserParam param) {
        UserVO vo = UserVO.builder()
                .userId(param.getId())
                .userName(param.getName())
                .age(27)
                .build();
        log.info("vo===={}", vo);
        return ResponseVO.success(vo);
    }
}
