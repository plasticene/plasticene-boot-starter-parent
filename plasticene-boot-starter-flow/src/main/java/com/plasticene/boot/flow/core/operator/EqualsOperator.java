package com.plasticene.boot.flow.core.operator;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FieldTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * @author ZFJ
 * @date 2025/9/3
 */
@Slf4j
public class EqualsOperator implements Operator {
    @Override
    public boolean compare(Integer fieldType, Object fieldValue, String inputValue) {
       try {
          if (fieldValue == null || inputValue == null) {
              return false;
          }
          


       } catch (Exception e) {
          log.error("equals operator error: fieldValue={}, inputValue={}", fieldValue, inputValue, e);
       }
       return false;
    }

    @Override
    public void validate(Integer filedType, String inputValue) {
        checkInputValue(filedType, inputValue);
        if (Objects.equals(filedType, FieldTypeEnum.COLLECTION.getCode())) {
            throw new BizException("集合类型不支持=比较");
        }

    }

    @Override
    public String op() {
        return "=";
    }
}
