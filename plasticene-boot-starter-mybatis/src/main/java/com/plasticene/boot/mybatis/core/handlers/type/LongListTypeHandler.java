package com.plasticene.boot.mybatis.core.handlers.type;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
public class LongListTypeHandler extends ListTypeHandler<Long>{
    @Override
    protected Long parseItem(String value) {
        return Long.parseLong(value.trim());
    }
}
