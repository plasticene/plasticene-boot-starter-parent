package com.plasticene.boot.mybatis.core.handlers;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.plasticene.boot.common.utils.JsonUtils;

import java.util.Set;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 10:49
 */
public class JsonLongSetTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final TypeReference<Set<Long>> typeReference = new TypeReference<Set<Long>>(){};

    @Override
    protected Object parse(String json) {
        return JsonUtils.parseObject(json, typeReference);
    }

    @Override
    protected String toJson(Object obj) {
        return JsonUtils.toJsonString(obj);
    }

}
