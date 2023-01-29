package com.plasticene.boot.common.user;

import lombok.Data;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/12/9 01:10
 */
@Data
public class LoginUser {
    private Long id;
    /**
     * 公司id
     */
    private Long orgId;

    /**
     * 是否是管理员 0：否 1：是
     */
    private Integer isAdmin;

    /**
     * 姓名
     */
    private String name;

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
