package com.plasticene.boot.mybatis.core.handlers;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.mybatis.core.metadata.BaseDO;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 公共字段属性值自动填充
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 10:38
 */


public class DefaultDBFieldHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if (Objects.nonNull(metaObject) && metaObject.getOriginalObject() instanceof BaseDO baseDO) {

            LocalDateTime now = LocalDateTime.now();
            // 创建时间为空，则以当前时间为插入时间
            if (Objects.isNull(baseDO.getCreateTime())) {
                baseDO.setCreateTime(now);
            }
            // 更新时间为空，则以当前时间为更新时间
            if (Objects.isNull(baseDO.getUpdateTime())) {
                baseDO.setUpdateTime(now);
            }

            LoginUser loginUser = LoginUserHolder.get();
            // 当前登录用户不为空，创建人为空，则当前登录用户为创建人
            if (Objects.nonNull(loginUser) && Objects.isNull(baseDO.getCreator())) {
                baseDO.setCreator(loginUser.getId());
            }
            // 当前登录用户不为空，更新人为空，则当前登录用户为更新人
            if (Objects.nonNull(loginUser) && Objects.isNull(baseDO.getUpdater())) {
                baseDO.setUpdater(loginUser.getId());
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间为空，则以当前时间为更新时间
        Object updateTime = getFieldValByName("updateTime", metaObject);
        if (Objects.isNull(updateTime)) {
            setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }

        LoginUser loginUser = LoginUserHolder.get();
        // 当前登录用户不为空，更新人为空，则当前登录用户为更新人
        Object modifier = getFieldValByName("updater", metaObject);
        if (Objects.nonNull(loginUser) && Objects.isNull(modifier)) {
            setFieldValByName("updater", loginUser.getId(), metaObject);
        }
    }
}
