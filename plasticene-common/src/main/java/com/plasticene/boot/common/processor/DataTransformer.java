package com.plasticene.boot.common.processor;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/9/30 10:19
 */
@FunctionalInterface
public interface DataTransformer<T, R> {

    /**
     * 数据转换
     * @param input
     * @return
     */
    R transform(T input);
}
