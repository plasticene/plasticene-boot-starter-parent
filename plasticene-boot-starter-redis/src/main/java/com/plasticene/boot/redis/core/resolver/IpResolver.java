package com.plasticene.boot.redis.core.resolver;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface IpResolver {

    /**
     * 解析客户端 IP 地址
     */
    String resolve();
}
