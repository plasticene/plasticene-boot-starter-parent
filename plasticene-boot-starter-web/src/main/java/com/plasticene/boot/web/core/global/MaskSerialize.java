package com.plasticene.boot.web.core.global;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.plasticene.boot.web.core.anno.FieldMask;
import com.plasticene.boot.web.core.enums.MaskEnum;
import com.plasticene.boot.web.core.utils.MaskUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/26 16:23
 */
public class MaskSerialize extends JsonSerializer<String> implements ContextualSerializer {

    /**
     * 脱敏类型
     */
    private MaskEnum type;


    @Override
    public void serialize(String s, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        switch (this.type) {
            case CHINESE_NAME:
            {
                jsonGenerator.writeString(MaskUtils.chineseName(s));
                break;
            }
            case ID_CARD:
            {
                jsonGenerator.writeString(MaskUtils.idCardNum(s));
                break;
            }
            case FIXED_PHONE:
            {
                jsonGenerator.writeString(MaskUtils.fixedPhone(s));
                break;
            }
            case MOBILE_PHONE:
            {
                jsonGenerator.writeString(MaskUtils.mobilePhone(s));
                break;
            }
            case ADDRESS:
            {
                jsonGenerator.writeString(MaskUtils.address(s, 4));
                break;
            }
            case EMAIL:
            {
                jsonGenerator.writeString(MaskUtils.email(s));
                break;
            }
            case BANK_CARD:
            {
                jsonGenerator.writeString(MaskUtils.bankCard(s));
                break;
            }
        }
    }

    @Override
    public JsonSerializer <?> createContextual(SerializerProvider serializerProvider, BeanProperty beanProperty) throws JsonMappingException {
        // 为空直接跳过
        if (beanProperty == null) {
            return serializerProvider.findNullValueSerializer(beanProperty);
        }
        // 非String类直接跳过
        if (Objects.equals(beanProperty.getType().getRawClass(), String.class)) {
            FieldMask fieldMask = beanProperty.getAnnotation(FieldMask.class);
            if (fieldMask == null) {
                fieldMask = beanProperty.getContextAnnotation(FieldMask.class);
            }
            if (fieldMask != null) {
                // 如果能得到注解，就将注解的 value 传入 MaskSerialize
                return new MaskSerialize(fieldMask.value());
            }
        }
        return serializerProvider.findValueSerializer(beanProperty.getType(), beanProperty);
    }

    public MaskSerialize() {}

    public MaskSerialize(final MaskEnum type) {
        this.type = type;
    }
}
