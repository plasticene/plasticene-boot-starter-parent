package com.plasticene.boot.flow.core.api.param;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * 保留流程表单中 JSON 小数的提交精度。
 * @author ZFJ
 * @since 2026-11-20
 */
public class FlowVariableMapDeserializer extends JsonDeserializer<Map<String, Object>> {

    private static final TypeReference<Map<String, Object>> TYPE = new TypeReference<>() { };

    @Override
    public Map<String, Object> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return ((ObjectMapper) parser.getCodec()).readerFor(TYPE)
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readValue(parser);
    }
}
