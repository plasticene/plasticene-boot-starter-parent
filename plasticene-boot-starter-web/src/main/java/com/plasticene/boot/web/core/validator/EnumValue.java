package com.plasticene.boot.web.core.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {EnumValueValidator.class})
public @interface EnumValue {

    String message() default "enum value is not valid";

    /** 关联的枚举类   */
    Class<? extends CheckEnumValue> linkEnum() default CheckEnumValue.class;

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
