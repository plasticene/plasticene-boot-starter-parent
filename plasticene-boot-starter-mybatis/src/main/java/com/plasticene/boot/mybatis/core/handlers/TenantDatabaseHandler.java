package com.plasticene.boot.mybatis.core.handlers;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.plasticene.boot.common.user.RequestUserHolder;
import com.plasticene.boot.mybatis.core.context.TenantContextHolder;
import com.plasticene.boot.mybatis.core.prop.TenantProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.HashSet;
import java.util.Set;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/12/10 18:29
 */
public class TenantDatabaseHandler implements TenantLineHandler {
    private final Set<String> ignoreTables = new HashSet<>();

    public TenantDatabaseHandler(TenantProperties properties) {
        // 大小写的习惯不同，所以需要都添加进去
        properties.getIgnoreTables().forEach(table -> {
            ignoreTables.add(table.toLowerCase());
            ignoreTables.add(table.toUpperCase());
        });
    }

    /**
     * 获取租户字段名
     * <p>
     * 默认字段名叫: tenant_id
     *
     * @return 租户字段名
     */
    @Override
     public String getTenantIdColumn() {
        return "org_id";
    }


    @Override
    public Expression getTenantId() {
        return new LongValue(RequestUserHolder.getCurrentUser().getOrgId());
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return TenantContextHolder.isIgnore()
                || CollUtil.contains(ignoreTables, tableName); // 情况二，忽略多租户的表
    }
}
