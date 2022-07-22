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
     * 接口限流切面
     */
    int AOP_RATE_LIMIT = HIGHEST_PRECEDENCE + 1;

    /**
     * 分布式锁切面
     */
    int AOP_LOCK = HIGHEST_PRECEDENCE + 2;


    /**
     * 日志trace过滤器
     */
    int FILTER_TRACE = Integer.MIN_VALUE;
}
