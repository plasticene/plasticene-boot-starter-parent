package com.plasticene.boot.mybatis.core.handlers;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.plasticene.boot.common.utils.JsonUtils;

import java.lang.reflect.Field;
import java.util.Set;

/**
 *
 * Set<String> 的类型转换器实现类，对应数据库的 varchar 类型
 *
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 11:58
 */
public class JsonStringSetTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final TypeReference<Set<String>> TYPE_REFERENCE = new TypeReference<>() {
    };

    public JsonStringSetTypeHandler(Class<?> type) {
        super(type);
    }

    public JsonStringSetTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    @Override
    public Object parse(String json) {
        return JsonUtils.parseObject(json, TYPE_REFERENCE);
    }

    @Override
    public String toJson(Object obj) {
        return JsonUtils.toJsonString(obj);
    }

}