package com.plasticene.boot.example.flow.leave;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;

/**
 * 运行无需 HTTP 登录态和 Redis 的请假审批接入示例。
 * @author ZFJ
 * @since 2026-11-11
 */
@Profile("leave")
@SpringBootApplication(excludeName = {
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "com.plasticene.boot.redis.autoconfigure.PlasticeneRedisAutoConfiguration",
        "com.plasticene.boot.web.autoconfigure.PlasticeneSwaggerAutoConfiguration",
        "com.plasticene.boot.web.autoconfigure.PlasticeneWebAutoConfiguration"})
public class LeaveExampleApplication {
    public static void main(String[] args) {
        try (var context = new SpringApplicationBuilder(LeaveExampleApplication.class)
                .profiles("leave").web(WebApplicationType.NONE).run(args)) {
            // ApplicationRunner 完成示例闭环后关闭宿主，测试使用独立上下文。
        }
    }
}
