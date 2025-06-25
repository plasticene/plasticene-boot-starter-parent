package com.plasticene.boot.mybatis.core.handlers.type;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
public abstract class SetTypeHandler<T> implements TypeHandler<Set<T>> {

    private static final String SEPARATOR = ",";

    @Override
    public void setParameter(PreparedStatement ps, int i, Set<T> parameter, JdbcType jdbcType) throws SQLException {
        // 分隔符
        ps.setString(i, CollUtil.join(parameter, SEPARATOR));
    }

    @Override
    public Set<T> getResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return parseSet(value);
    }

    @Override
    public Set<T> getResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return parseSet(value);
    }

    @Override
    public Set<T> getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return parseSet(value);
    }

    private Set<T> parseSet(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return StrUtil.split(value, SEPARATOR).stream()
                .map(this::parseItem)
                .collect(Collectors.toSet());
    }

    // 子类需实现具体类型的转换逻辑
    protected abstract T parseItem(String value);
}
