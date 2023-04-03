package com.plasticene.boot.common.user;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/12/9 01:16
 */
public class RequestUserHolder {
    private static final ThreadLocal<LoginUser> userHolder = new TransmittableThreadLocal<>();

    /**
     * 存储用户信息
     *
     * @param loginUser
     */
    public static void add(LoginUser loginUser) {
        userHolder.set(loginUser);
    }

    /**
     * 获取用户信息
     *
     * @return
     */
    public static LoginUser getCurrentUser() {
        return userHolder.get();
    }

    /**
     * 清除
     */
    public static void remove() {
        userHolder.remove();
    }
}
