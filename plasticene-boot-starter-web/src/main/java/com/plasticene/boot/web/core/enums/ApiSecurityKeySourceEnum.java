package com.plasticene.boot.web.core.enums;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2025/5/23
 * 接口参数加解密key的来源方式枚举
 */
public enum ApiSecurityKeySourceEnum {

    /**
     * 公共 加解密的key 接口提供方和调用方都共用一个key
     * 这是方式可以直接在配置文件配置，简单直接，但是安全性不高
     */
    COMMON,

    /**
     * 单独 第三方平台调用我方接口，我方会单独为每一个平台生成加解密的公钥秘钥key
     * 同时第三方也要也要提供他们自己key供我方进行接口响应结果参数加密返回
     * 详见 {@link com.plasticene.boot.web.core.model.ApiSecurityKey}
     * 单独的时候，需要业务侧把apiSecurityKey信息作为上下文传递
     */
    ALONE;
}
