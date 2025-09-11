package com.plasticene.boot.flow.core.operator;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.enums.FieldTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Date;
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
          if (Objects.equals(fieldType, FieldTypeEnum.NUMBER.getCode())) {
              // 数字类型统一转换成bigDecimal，防止小数精度丢失
              BigDecimal value = new BigDecimal(fieldValue.toString());
              BigDecimal input = new BigDecimal(inputValue);
              return input.compareTo(value) == 0;
          }
          if (Objects.equals(fieldType, FieldTypeEnum.DATE.getCode())) {
              // 日期时间统一转换为时间戳, 好比较
              long timestamp = getTimestamp(fieldValue);
              return Objects.equals(timestamp, Long.valueOf(inputValue));
          }
          return Objects.equals(fieldValue.toString(), inputValue);
       } catch (Exception e) {
          log.error("【{}】operator error: fieldValue={}, inputValue={}", op(), fieldValue, inputValue, e);
          throw e;
       }
    }

    @Override
    public void validate(Integer fieldType, String inputValue) {
        checkInputValue(fieldType, inputValue);
        checkNumberOperator(fieldType);
    }

    @Override
    public String op() {
        return "=";
    }
}
