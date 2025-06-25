package com.plasticene.boot.example.mybatis.service;

import com.plasticene.boot.example.mybatis.dao.UserDAO;
import com.plasticene.boot.example.mybatis.entity.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
@Service
public class UserService {
    @Resource
    private UserDAO userDAO;



    public User getUserById(Long id) {
        return userDAO.selectById(id);
    }

    public User getUser(Long id) {
        return userDAO.getById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void insertUser(User user) {
        userDAO.insert(user);
    }



}
