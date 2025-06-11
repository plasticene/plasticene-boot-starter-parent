package com.plasticene.boot.example.redis.controller;

import com.alibaba.fastjson.JSON;
import com.plasticene.boot.common.pojo.ResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/11
 */
@RestController
@Tag(name = "redis测试案例")
public class TestController {
    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "测试redisTemplate序列化")
    @GetMapping("/redisTemplate")
    public ResponseVO<List<String>> testRedisTemplate() {
        ResponseVO<List<String>> vo = ResponseVO.success(List.of("哈哈", "😄hello"));
        redisTemplate.opsForValue().set("she-001", vo);
        return vo;
    }

    @Operation(summary = "测试stringRedisTemplate序列化")
    @GetMapping("/stringRedisTemplate")
    public ResponseVO<List<String>> testStringRedisTemplate() {
        ResponseVO<List<String>> vo = ResponseVO.success(List.of("呵呵", "😄hi"));
        stringRedisTemplate.opsForValue().set("she-002", JSON.toJSONString(vo), 3, TimeUnit.MINUTES);
        return vo;
    }
}
