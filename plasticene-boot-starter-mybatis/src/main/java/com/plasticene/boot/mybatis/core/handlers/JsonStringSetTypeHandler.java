package com.plasticene.boot.mybatis.core.handlers;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.plasticene.boot.common.utils.JsonUtils;

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

    private static final TypeReference<Set<String>> typeReference = new TypeReference<Set<String>>(){};

    @Override
    protected Object parse(String json) {
        return JsonUtils.parseObject(json, typeReference);
    }

    @Override
    protected String toJson(Object obj) {
        return JsonUtils.toJsonString(obj);
    }

}