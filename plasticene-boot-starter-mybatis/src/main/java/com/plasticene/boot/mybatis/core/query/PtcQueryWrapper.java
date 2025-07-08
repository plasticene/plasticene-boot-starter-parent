package com.plasticene.boot.mybatis.core.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 *
 * 扩展mybatis-plus的查询封装，提供流式拼接条件  xxxIfPresent
 *
 *
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 13:47
 */
public class PtcQueryWrapper<T> extends QueryWrapper<T> {

    public PtcQueryWrapper<T> likeIfPresent(String column, String val) {
        if (StringUtils.hasText(val)) {
            return (PtcQueryWrapper<T>) super.like(column, val);
        }
        return this;
    }

    public PtcQueryWrapper<T> inIfPresent(String column, Collection<?> values) {
        if (!CollectionUtils.isEmpty(values)) {
            return (PtcQueryWrapper<T>) super.in(column, values);
        }
        return this;
    }

    public PtcQueryWrapper<T> inIfPresent(String column, Object... values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (PtcQueryWrapper<T>) super.in(column, values);
        }
        return this;
    }

    public PtcQueryWrapper<T> eqIfPresent(String column, Object val) {
        if (val != null) {
            return (PtcQueryWrapper<T>) super.eq(column, val);
        }
        return this;
    }

    public PtcQueryWrapper<T> neIfPresent(String column, Object val) {
        if (val != null) {
            return (PtcQueryWrapper<T>) super.ne(column, val);
        }
        return this;
    }

    public PtcQueryWrapper<T> gtIfPresent(String column, Object val) {
        if (val != null) {
            return (PtcQueryWrapper<T>) super.gt(column, val);
        }
        return this;
    }

    public PtcQueryWrapper<T> geIfPresent(String column, Object val) {
        if (val != null) {
            return (PtcQueryWrapper<T>) super.ge(column, val);
        }
        return this;
    }

    public PtcQueryWrapper<T> ltIfPresent(String column, Object val) {
        if (val != null) {
            return (PtcQueryWrapper<T>) super.lt(column, val);
        }
        return this;
    }

    public PtcQueryWrapper<T> leIfPresent(String column, Object val) {
        if (val != null) {
            return (PtcQueryWrapper<T>) super.le(column, val);
        }
        return this;
    }

    public PtcQueryWrapper<T> betweenIfPresent(String column, Object val1, Object val2) {
        if (val1 != null && val2 != null) {
            return (PtcQueryWrapper<T>) super.between(column, val1, val2);
        }
        if (val1 != null) {
            return (PtcQueryWrapper<T>) ge(column, val1);
        }
        if (val2 != null) {
            return (PtcQueryWrapper<T>) le(column, val2);
        }
        return this;
    }

    // ========== 重写父类方法，方便链式调用 ==========

    @Override
    public PtcQueryWrapper<T> eq(boolean condition, String column, Object val) {
        super.eq(condition, column, val);
        return this;
    }

    @Override
    public PtcQueryWrapper<T> eq(String column, Object val) {
        super.eq(column, val);
        return this;
    }

    @Override
    public PtcQueryWrapper<T> orderByDesc(String column) {
        super.orderByDesc(true, column);
        return this;
    }

    @Override
    public PtcQueryWrapper<T> last(String lastSql) {
        super.last(lastSql);
        return this;
    }

    @Override
    public PtcQueryWrapper<T> in(String column, Collection<?> coll) {
        super.in(column, coll);
        return this;
    }

}
