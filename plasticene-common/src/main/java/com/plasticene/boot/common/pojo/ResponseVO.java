package com.plasticene.boot.common.pojo;

import com.plasticene.boot.common.enums.ResponseCodeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 11:33
 */
@Data
public class ResponseVO<T> implements Serializable {

    private Integer code;

    private String msg;

    private T data;

    public ResponseVO() {

    }

    public ResponseVO(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ResponseVO(Integer code, T data) {
        this.code = code;
        this.data = data;
    }

    public ResponseVO(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    private ResponseVO(ResponseCodeEnum resultStatus, T data) {
        this.code = resultStatus.getCode();
        this.msg = resultStatus.getMsg();
        this.data = data;
    }

    /**
     * 业务成功返回业务代码和描述信息
     */
    public static ResponseVO<Void> success() {
        return new ResponseVO<>(ResponseCodeEnum.SUCCESS, null);
    }

    /**
     * 业务成功返回业务代码,描述和返回的参数
     */
    public static <T> ResponseVO<T> success(T data) {
        return new ResponseVO<>(ResponseCodeEnum.SUCCESS, data);
    }

    /**
     * 业务成功返回业务代码,描述和返回的参数
     */
    public static <T> ResponseVO<T> success(ResponseCodeEnum resultStatus, T data) {
        if (resultStatus == null) {
            return success(data);
        }
        return new ResponseVO<T>(resultStatus, data);
    }

    /**
     * 业务异常返回业务代码和描述信息
     */
    public static <T> ResponseVO<T> failure() {
        return new ResponseVO<>(ResponseCodeEnum.SYSTEM_ERROR, null);
    }

    /**
     * 业务异常返回业务代码,描述和返回的参数
     */
    public static <T> ResponseVO<T> failure(ResponseCodeEnum resultStatus) {
        return failure(resultStatus, null);
    }

    /**
     * 业务异常返回业务代码,描述和返回的参数
     */
    public static <T> ResponseVO<T> failure(ResponseCodeEnum resultStatus, T data) {
        if (resultStatus == null) {
            return new ResponseVO<>(ResponseCodeEnum.SYSTEM_ERROR, null);
        }
        return new ResponseVO<>(resultStatus, data);
    }

    public static <T> ResponseVO<T> failure(Integer code, String msg) {
        return new ResponseVO<>(code, msg);
    }
}

