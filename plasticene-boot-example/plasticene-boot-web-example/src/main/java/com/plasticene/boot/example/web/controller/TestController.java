package com.plasticene.boot.example.web.controller;

import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.example.web.param.UserParam;
import com.plasticene.boot.example.web.vo.UserVO;
import com.plasticene.boot.web.core.anno.ApiLog;
import com.plasticene.boot.web.core.anno.ResponseResultBody;
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
@ApiLog
@ResponseResultBody
public class TestController {

    @Operation(summary = "获取消息")
    @GetMapping("/msg")
    public ResponseVO<String> getMessage() {
        return ResponseVO.success("哈哈😄");
    }

    @Operation(summary = "测试swagger3文档、traceId、api接口出入参数输出")
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

    @Operation(summary = "获取用户信息(测试响应结果结构统一格式)")
    @GetMapping("/user")
    public UserVO getUser() {
        return UserVO.builder().userId(10L).userName("plasticene").build();
    }
}
