package com.plasticene.boot.common.user;

import lombok.Data;

import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/12/9 01:10
 */
@Data
public class LoginUser {
    /**
     * 登录用户id
     */
    private Long id;
    /**
     * 登陆用户部门id
     */
    private Long deptId;
    /**
     * 登陆用户角色
     */
    private List<Long> roleIds;
    /**
     * 租户id
     */
    private Long orgId;
    /**
     * 是否是管理员 0：否 1：是
     */
    private Integer isAdmin;
    /**
     * 登录用户名
     */
    private String username;
    /**
     * 昵称(姓名)
     */
    private String nickname;
    /**
     * 性别 0：男  1：女
     */
    private Integer gender;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * token令牌
     */
    private String token;


}
