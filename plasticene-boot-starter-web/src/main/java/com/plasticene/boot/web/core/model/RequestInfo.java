package com.plasticene.boot.web.core.model;

import lombok.Data;

import java.io.Serializable;

/**
 *
 * 请求信息
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/18 11:29
 */
@Data
public class RequestInfo implements Serializable {
    private String ip;
    private String url;
    private String httpMethod;
    private String classMethod;
    private Object requestParams;
}

