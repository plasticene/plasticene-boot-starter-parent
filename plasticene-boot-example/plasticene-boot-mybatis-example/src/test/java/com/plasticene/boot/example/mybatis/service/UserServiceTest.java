package com.plasticene.boot.example.mybatis.service;

import cn.hutool.core.date.DateUtil;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.RequestUserHolder;
import com.plasticene.boot.example.mybatis.entity.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;


/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/6/24
 */
@SpringBootTest
@RunWith(SpringRunner.class)
class UserServiceTest {
    @Resource
    private UserService userService;

    /**
     * 测试连通性
     */
    @Test
    public void getUserById() {
        User user = userService.getUserById(5201021L);
        System.out.println(user);
    }

    /**
     * 测试实体类公共属性填充
     */
    @Test
    public void testFillBaseDO() {
        User user = new User();
        user.setUserNo(UUID.randomUUID().toString());
        user.setName("公共属性填充测试");
        user.setBirthday(DateUtil.offsetDay(new Date(), 5));
        user.setPhone("123456789");
        userService.insertUser(user);
    }

    /**
     * 测试类型处理器器
     */
    @Test
    public void testTypeHandler() {
        User user = new User();
        user.setUserNo(UUID.randomUUID().toString());
        user.setName("类型处理器");
        user.setPhone("123456789");
        user.setRoleId(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));
        user.setHobby(List.of("网球", "篮球", "⚽️"));
        user.setRemark(Map.of("city", "杭州", "age",18, "idCard","132x"));
        userService.insertUser(user);
    }

    /**
     * 测试类型转换数据查询，实体类映射查询请看{@link UserServiceTest#getUserById()}
     * 这里测试通过XML写SQL语句查询的, 没有写<resultMap></resultMap> 复合字段不会转换，值都是null
     */
    @Test
    public void testGetUserByXmlSQL() {
        User user = userService.getUser(5201021L);
        System.out.println(user);
    }

    /**
     * 测试字段加密存储
     * 数据查询请看，实体类映射查询请看{@link UserServiceTest#getUserById()}
     * xml写SQL语句，{@link UserServiceTest#testGetUserByXmlSQL()}
     */
    @Test
    public void testEncrypt() {
        User user = new User();
        user.setUserNo(UUID.randomUUID().toString());
        user.setName("加密存储：哈哈123，⚽️");
        user.setPhone("010-123456789");
        user.setEmail("123456@qq.com");
        user.setRoleId(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));
        user.setHobby(List.of("网球", "篮球", "⚽️"));
        user.setRemark(Map.of("city", "杭州", "age",18, "idCard","132x"));
        user.setOrgId(6L);
        userService.insertUser(user);
    }


    /**
     * 测试分页
     */
    @Test
    public void testPage() {
        PageResult<User> result = userService.listUsers(2, 10, "吴八花");
        System.out.println(result);
    }

    /**
     * 测试多租户
     */
    @Test
    public void testTenant() {
        LoginUser loginUser = new LoginUser();
        loginUser.setOrgId(6L);
        RequestUserHolder.add(loginUser);
        PageResult<User> result = userService.listUsers(1, 5, null);
        System.out.println(result);
        RequestUserHolder.remove();
    }


}