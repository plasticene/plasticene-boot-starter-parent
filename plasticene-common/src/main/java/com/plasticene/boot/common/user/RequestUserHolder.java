package com.plasticene.boot.common.user;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/12/9 01:16
 */
public class RequestUserHolder {
    private static final ThreadLocal<LoginUser> LOGIN_USER_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 存储用户信息
     */
    public static void setLoginUser(LoginUser loginUser) {
        LOGIN_USER_HOLDER.set(loginUser);
    }

    /**
     * 获取用户信息
     */
    public static LoginUser getLoginUser() {
        return LOGIN_USER_HOLDER.get();
    }

    /**
     * 清除
     */
    public static void remove() {
        LOGIN_USER_HOLDER.remove();
    }
}
