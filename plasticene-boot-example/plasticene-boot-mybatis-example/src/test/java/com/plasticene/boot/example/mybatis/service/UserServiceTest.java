package com.plasticene.boot.example.mybatis.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.example.mybatis.dao.UserDAO;
import com.plasticene.boot.example.mybatis.entity.User;
import com.plasticene.boot.mybatis.core.generator.CodeGenerator;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
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
    @Resource
    private UserDAO userDAO;
    @Resource
    private CodeGenerator codeGenerator;

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
        LoginUserHolder.set(loginUser);
        PageResult<User> result = userService.listUsers(1, 5, null);
        System.out.println(result);
        LoginUserHolder.remove();
    }

    @Test
    public void testBatchUpdateByFor() {
        List<User> users = listUsers();
        long start = System.currentTimeMillis();
        users.forEach(user -> userDAO.updateById(user));
        long end = System.currentTimeMillis();
        System.out.println("执行时长：" + (end - start) + "ms");
    }


    @Test
    public void testBatchUpdateByForeach() {
        List<User> users = listUsers();
        long start = System.currentTimeMillis();
        int sum = 0;
        // 分批处理
        List<List<User>> splitList = CollUtil.split(users, 500);
        for (List<User> userList : splitList) {
            int count = userDAO.batchUpdateByForeach(userList);
            sum = sum + count;
        }
        long end = System.currentTimeMillis();
        System.out.println("执行时长：" + (end - start) + "ms");
        // 注意sum=20，而不是10000
        System.out.println("sum:" + sum);
    }

    @Test
    public void testBatchUpdateByCaseWhen() {
        List<User> users = listUsers();
        long start = System.currentTimeMillis();
        // 分批处理
        List<List<User>> splitList = CollUtil.split(users, 500);
        for (List<User> userList : splitList) {
            userDAO.batchUpdateByCaseWhen(userList);
        }
        long end = System.currentTimeMillis();
        System.out.println("执行时长：" + (end - start) + "ms");
    }


    @Test
    public void testBatchUpdateOnDuplicate() {
        List<User> users = listUsers();
        long start = System.currentTimeMillis();
        // 分批处理
        List<List<User>> splitList = CollUtil.split(users, 500);
        for (List<User> userList : splitList) {
            userDAO.batchUpdateOnDuplicate(userList);
        }
        long end = System.currentTimeMillis();
        System.out.println("执行时长：" + (end - start) + "ms");
    }

    @Test
    public void testBatchUpdateReplace() {
        List<User> users = listUsers();
        long start = System.currentTimeMillis();
        // 分批处理
        List<List<User>> splitList = CollUtil.split(users, 500);
        for (List<User> userList : splitList) {
            userDAO.batchUpdateReplace(userList);
        }
        long end = System.currentTimeMillis();
        System.out.println("执行时长：" + (end - start) + "ms");
    }

    @Test
    public void testBatchUpdateByMybatisPlus() {
        List<User> users = listUsers();
        long start = System.currentTimeMillis();
        userDAO.updateById(users, 500);
//        userService.updateBatchById(users, 500);
        // 这个就是for循环单条操作，很慢
//        userService.saveOrUpdateBatch(users, 500);
        long end = System.currentTimeMillis();
        System.out.println("执行时长：" + (end - start) + "ms");
    }








    public List<User> listUsers() {
        PtcLambdaQueryWrapper<User> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.select(User::getId, User::getName, User::getPhone);
        queryWrapper.ge(User::getId, 20000L).lt(User::getId, 30000L);
        List<User> users = userDAO.selectList(queryWrapper);
        users.forEach(user -> {
//            if (user.getId() % 100 == 0) {
//                user.setId(null);
//                user.setUserNo(UUID.randomUUID().toString());
//            }
            user.setName(user.getName() + "1");
            user.setAddress("杭州" + user.getId());
            user.setGender(user.getId() % 2 == 0 ? 1 : 0);
            user.setUpdateTime(LocalDateTime.now());
            user.setUpdater(user.getId());
        });
        return users;
    }

    @Test
    public void testCodeGenerator() {
        codeGenerator.generate("zfj", List.of("visit_score"), null);

    }


}