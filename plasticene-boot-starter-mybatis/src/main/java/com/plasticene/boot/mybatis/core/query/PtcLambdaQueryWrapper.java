package com.plasticene.boot.mybatis.core.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 13:51
 */
public class PtcLambdaQueryWrapper<T> extends LambdaQueryWrapper<T> {

    public PtcLambdaQueryWrapper<T> likeIfPresent(SFunction<T, ?> column, String val) {
        if (StringUtils.hasText(val)) {
            return (PtcLambdaQueryWrapper<T>) super.like(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> likeRightIfPresent(SFunction<T, ?> column, String val) {
        if (StringUtils.hasText(val)) {
            return (PtcLambdaQueryWrapper<T>) super.likeRight(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> likeLeftIfPresent(SFunction<T, ?> column, String val) {
        if (StringUtils.hasText(val)) {
            return (PtcLambdaQueryWrapper<T>) super.likeLeft(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> inIfPresent(SFunction<T, ?> column, Collection<?> values) {
        if (!CollectionUtils.isEmpty(values)) {
            return (PtcLambdaQueryWrapper<T>) super.in(column, values);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> inIfPresent(SFunction<T, ?> column, Object... values) {
        if (!ArrayUtils.isEmpty(values)) {
            return (PtcLambdaQueryWrapper<T>) super.in(column, values);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> eqIfPresent(SFunction<T, ?> column, Object val) {
        if (val != null) {
            return (PtcLambdaQueryWrapper<T>) super.eq(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> neIfPresent(SFunction<T, ?> column, Object val) {
        if (val != null) {
            return (PtcLambdaQueryWrapper<T>) super.ne(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> gtIfPresent(SFunction<T, ?> column, Object val) {
        if (val != null) {
            return (PtcLambdaQueryWrapper<T>) super.gt(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> geIfPresent(SFunction<T, ?> column, Object val) {
        if (val != null) {
            return (PtcLambdaQueryWrapper<T>) super.ge(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> ltIfPresent(SFunction<T, ?> column, Object val) {
        if (val != null) {
            return (PtcLambdaQueryWrapper<T>) super.lt(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> leIfPresent(SFunction<T, ?> column, Object val) {
        if (val != null) {
            return (PtcLambdaQueryWrapper<T>) super.le(column, val);
        }
        return this;
    }

    public PtcLambdaQueryWrapper<T> betweenIfPresent(SFunction<T, ?> column, Object val1, Object val2) {
        if (val1 != null && val2 != null) {
            return (PtcLambdaQueryWrapper<T>) super.between(column, val1, val2);
        }
        if (val1 != null) {
            return (PtcLambdaQueryWrapper<T>) ge(column, val1);
        }
        if (val2 != null) {
            return (PtcLambdaQueryWrapper<T>) le(column, val2);
        }
        return this;
    }

    // ========== 重写父类方法，方便链式调用 ==========

    @Override
    public PtcLambdaQueryWrapper<T> eq(boolean condition, SFunction<T, ?> column, Object val) {
        super.eq(condition, column, val);
        return this;
    }

    @Override
    public PtcLambdaQueryWrapper<T> eq(SFunction<T, ?> column, Object val) {
        super.eq(column, val);
        return this;
    }

    @Override
    public PtcLambdaQueryWrapper<T> orderByDesc(SFunction<T, ?> column) {
        super.orderByDesc(true, column);
        return this;
    }

    @Override
    public PtcLambdaQueryWrapper<T> last(String lastSql) {
        super.last(lastSql);
        return this;
    }

    @Override
    public PtcLambdaQueryWrapper<T> in(SFunction<T, ?> column, Collection<?> coll) {
        super.in(column, coll);
        return this;
    }

}

