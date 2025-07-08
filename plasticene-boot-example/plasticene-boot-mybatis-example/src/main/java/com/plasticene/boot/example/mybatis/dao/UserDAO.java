package com.plasticene.boot.example.mybatis.dao;

import com.plasticene.boot.example.mybatis.entity.User;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
public interface UserDAO extends BaseMapperX<User> {

    User getById(Long id);

    int batchUpdateByForeach(@Param("userList") List<User> userList);

    int batchUpdateByCaseWhen(@Param("userList") List<User> userList);

    int batchUpdateOnDuplicate(@Param("userList") List<User> userList);

    int batchUpdateReplace(@Param("userList") List<User> userList);




}
