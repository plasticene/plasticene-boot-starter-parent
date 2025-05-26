package com.plasticene.boot.web.core.validator;

import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 */
public interface CheckEnumValue<T> {

    /**
     * 需要校验的枚举类都需要实现该接口返回所有枚举值
     * @return 枚举值
     */
    List<T> getEnumValue();

}
