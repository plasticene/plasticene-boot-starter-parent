package com.plasticene.boot.web.core.anno;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.plasticene.boot.web.core.enums.MaskEnum;
import com.plasticene.boot.web.core.global.MaskSerialize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/26 16:20
 */

@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = MaskSerialize.class)
public @interface FieldMask {

    /**
     * 脱敏类型
     * @return
     */
    MaskEnum value();
}
