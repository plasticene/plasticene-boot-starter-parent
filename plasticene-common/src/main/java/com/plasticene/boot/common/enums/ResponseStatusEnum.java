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
public enum ResponseStatusEnum {
    FORBIDDEN(HttpStatus.HTTP_FORBIDDEN,  "Forbidden"),
    UNAUTHORIZED(HttpStatus.HTTP_UNAUTHORIZED, "Unauthorized"),
    SUCCESS(HttpStatus.HTTP_OK, "OK"),
    BAD_REQUEST(HttpStatus.HTTP_BAD_REQUEST, "Bad Request"),
    SYSTEM_ERROR(HttpStatus.HTTP_INTERNAL_ERROR, "系统异常错误"),
    NOT_EXIST(HttpStatus.HTTP_NOT_FOUND, "请求路径不存在");


    /**
     * 返回的HTTP状态码,  符合http请求
     */
    private HttpStatus httpStatus;
    /**
     * 业务异常码
     */
    private Integer code;
    /**
     * 业务异常信息描述
     */
    private String msg;

    ResponseStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }


}
