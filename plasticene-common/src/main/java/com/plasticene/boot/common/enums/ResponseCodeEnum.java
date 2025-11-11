package com.plasticene.boot.common.enums;

import cn.hutool.http.HttpStatus;
import lombok.Getter;
import lombok.ToString;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 11:34
 */
@ToString
@Getter
public enum ResponseCodeEnum {

    SUCCESS(0, "成功"),
    UNAUTHORIZED(401, "账号未登录"),
    BAD_REQUEST(400, "请求参数不正确"),
    FORBIDDEN(403,  "操作没权限"),
    NOT_FOUND(404, "请求未找到"),
    SYSTEM_ERROR(500, "系统错误");



    /**
     * 响应码
     * 按照http规范：{@link HttpStatus}， 成功除外，成功-0
     */
    private final int code;
    /**
     * 响应码信息
     */
    private final String msg;

    ResponseCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


}
