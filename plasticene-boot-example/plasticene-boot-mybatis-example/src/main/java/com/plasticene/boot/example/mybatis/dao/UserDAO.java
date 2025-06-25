package com.plasticene.boot.example.mybatis.dao;

import com.plasticene.boot.example.mybatis.entity.User;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
public interface UserDAO extends BaseMapperX<User> {

    User getById(Long id);
}
