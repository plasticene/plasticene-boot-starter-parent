package com.plasticene.boot.web.core.model;

import lombok.Data;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/5/4 11:17
 */
@Data
public class ApiSecurityParam {

    /**
     * 应用id
     */
    private String appId;

    /**
     * RSA加密后的aes秘钥，需解密
     */
    private String key;

    /**
     * AES加密的json参数
     */
    private String data;

    /**
     * 签名
     */
    private String sign;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 请求唯一标识
     */
    private String nonce;

}
