package com.plasticene.boot.mybatis.core.handlers;

import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.mybatis.core.encrypt.EncryptService;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import javax.annotation.Resource;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/22 14:37
 */
public class EncryptTypeHandler<T> extends BaseTypeHandler<T> {

    @Resource
    private EncryptService encryptService;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, encryptService.encrypt((String)parameter));
    }
    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String columnValue = rs.getString(columnName);
        //有一些可能是空字符
        return StrUtil.isBlank(columnValue) ? (T)columnValue : (T)encryptService.decrypt(columnValue);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnValue = rs.getString(columnIndex);
        return StrUtil.isBlank(columnValue) ? (T)columnValue : (T)encryptService.decrypt(columnValue);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String columnValue = cs.getString(columnIndex);
        return StrUtil.isBlank(columnValue) ? (T)columnValue : (T)encryptService.decrypt(columnValue);
    }
}

