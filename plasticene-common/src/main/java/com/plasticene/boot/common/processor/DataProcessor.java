package com.plasticene.boot.common.processor;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/9/30 10:28
 */
public class DataProcessor {
    public static <T, R> R processData(T data, DataTransformer<T, R> transformer) {
        return transformer.transform(data);
    }

    public static void main(String[] args) {
        String s = DataProcessor.processData("hello world", String::toUpperCase);
        System.out.println(s);

    }
}
