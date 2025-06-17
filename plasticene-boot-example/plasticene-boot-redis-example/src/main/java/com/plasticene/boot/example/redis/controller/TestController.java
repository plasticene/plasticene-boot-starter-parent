package com.plasticene.boot.example.redis.controller;

import com.alibaba.fastjson.JSON;
import com.plasticene.boot.cache.core.manager.MultilevelCache;
import com.plasticene.boot.common.pojo.ResponseVO;
import com.plasticene.boot.redis.core.anno.DistributedLock;
import com.plasticene.boot.redis.core.anno.RateLimit;
import com.plasticene.boot.redis.core.enums.LimitType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/11
 */
@RestController
@Tag(name = "redis测试案例")
@RequestMapping("/test")
@Slf4j
public class TestController {
    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MultilevelCache multilevelCache;


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

    @Operation(summary = "测试分布式锁")
    @GetMapping("/lock")
    @DistributedLock(name = "user", key = "001")
    public ResponseVO<String> testDistributedLock() {
        log.info("加锁成功，执行业务...{}", Thread.currentThread().getName());
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            log.error("error", e);
        }
        return ResponseVO.success("成功");
    }


    @Operation(summary = "测试默认限流")
    @GetMapping("/limit")
    @RateLimit(period = 30, count = 3)
    public int testLimiter() {
        return 1;
    }


    @Operation(summary = "测试自定义限流")
    @GetMapping("/limit/custom")
    @RateLimit(key = "custom_limit_test", period = 30, count = 3, limitType = LimitType.CUSTOM)
    public int testCustomLimiter() {
        return 1;
    }


    @Operation(summary = "测试ip限流")
    @GetMapping("/limit/ip")
    @RateLimit(key = "ip_limit_test", period = 30, count = 3, limitType = LimitType.IP, prefix = "hello")
    public int testIpLimiter() {
        return 1;
    }

    @Operation(summary = "测试多级缓存")
    @GetMapping("/multilevel")
    public ResponseVO<String> testMultilevelCache() {
        String value = multilevelCache.get("multi-key001", () -> UUID.randomUUID().toString());
        return ResponseVO.success(value);
    }

    @Operation(summary = "测试spring cache")
    @GetMapping("/cache")
    @Cacheable(value = "she-cache")
    public ResponseVO<String> testSpringCache() {
        log.info("执行方法了.....");
        return ResponseVO.success("成功😄");
    }
}
