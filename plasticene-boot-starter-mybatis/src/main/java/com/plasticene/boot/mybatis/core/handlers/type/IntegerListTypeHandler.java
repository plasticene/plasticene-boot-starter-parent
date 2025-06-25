package com.plasticene.boot.mybatis.core.handlers.type;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
public class IntegerListTypeHandler extends ListTypeHandler<Integer>{
    @Override
    protected Integer parseItem(String value) {
        return Integer.parseInt(value.trim());
    }
}
