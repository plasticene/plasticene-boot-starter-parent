package com.plasticene.boot.example.web.enums;

import com.plasticene.boot.web.core.validator.CheckEnumValue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 */
public enum GenderEnum implements CheckEnumValue<Integer> {

    MAN(0, "男生"),
    WOMAN(1, "女生");

    private final Integer code;

    private final String name;

    GenderEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public List<Integer> getEnumValue() {
        return Stream.of(GenderEnum.values())
                .map(genderEnum -> genderEnum.code)
                .collect(Collectors.toList());
    }
}

