package com.plasticene.boot.mybatis.core.handlers.type;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
public class StringListTypeHandler extends ListTypeHandler<String> {
    @Override
    protected String parseItem(String value) {
        return value.trim();
    }
}
