package com.plasticene.boot.example.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.pojo.PageParam;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.example.mybatis.dao.UserDAO;
import com.plasticene.boot.example.mybatis.entity.User;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
@Service
public class UserService extends ServiceImpl<UserDAO, User> {
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

    public PageResult<User> listUsers(Integer pageNo, Integer pageSize, String name) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.eq(User::getName, name);
        }
        PageParam param = new PageParam(pageNo, pageSize);
        return userDAO.selectPage(param, queryWrapper);
    }





}
