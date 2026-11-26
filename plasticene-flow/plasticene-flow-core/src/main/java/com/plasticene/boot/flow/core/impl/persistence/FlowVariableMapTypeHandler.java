package com.plasticene.boot.flow.core.impl.persistence;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 以原始十进制精度读取实例变量，供表单保存和业务字段同步使用。
 * @author ZFJ
 * @since 2026-11-20
 */
public class FlowVariableMapTypeHandler extends JacksonTypeHandler {

    private static final TypeReference<Map<String, Object>> TYPE = new TypeReference<>() { };

    public FlowVariableMapTypeHandler(Class<?> type) {
        super(type);
    }

    public FlowVariableMapTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    @Override
    public Object parse(String json) {
        try {
            return getObjectMapper().readerFor(TYPE).with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .readValue(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("流程实例变量格式不正确", exception);
        }
    }
}
