package com.plasticene.boot.mybatis.core.handlers.type;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 * 用于处理数据库字段与Java Set<Integer>类型之间的转换  供项目方使用
 */
@SuppressWarnings("unused")
public class IntegerSetTypeHandler extends SetTypeHandler<Integer>{
    @Override
    protected Integer parseItem(String value) {
        return Integer.parseInt(value.trim());
    }
}
