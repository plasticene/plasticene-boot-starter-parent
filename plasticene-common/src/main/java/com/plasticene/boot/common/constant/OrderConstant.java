package com.plasticene.boot.common.constant;

/**
 *
 * 注意 注意  注意
 * 这里常量类型用于注解时，数据类型要和注解属性类型一直，不要发生包装类转换这些，不然编译报错。
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/22 10:31
 */
public interface OrderConstant {

    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE + 10000;
    /**
     * 接口访问日志打印切面
     */
    int AOP_API_LOG = HIGHEST_PRECEDENCE;

    /**
     * 接口参数加解密切面
     */
    int AOP_API_DECRYPT = HIGHEST_PRECEDENCE + 9;

    /**
     * 接口参数签名验证
     */
    int AOP_API_SIGN_VERIFY = HIGHEST_PRECEDENCE + 10;

    /**
     * license验证切面
     */
    int AOP_LICENSE = HIGHEST_PRECEDENCE + 100;

    /**
     * 接口限流切面
     */
    int AOP_RATE_LIMIT = HIGHEST_PRECEDENCE + 200;

    /**
     * 分布式锁切面
     */
    int AOP_LOCK = HIGHEST_PRECEDENCE + 300;

    /**
     * 日志trace过滤器
     */
    int FILTER_TRACE = Integer.MIN_VALUE + 100;

    /**
     * license启动runner
     */
    int RUNNER_LICENSE = HIGHEST_PRECEDENCE + 100;

    /**
     * 图案信息打印runner
     */
    int RUNNER_BANNER = HIGHEST_PRECEDENCE + 1000;
}
