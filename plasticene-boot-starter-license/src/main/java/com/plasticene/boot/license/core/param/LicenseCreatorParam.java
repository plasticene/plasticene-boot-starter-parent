package com.plasticene.boot.license.core.param;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 14:53
 */
@Data
public class LicenseCreatorParam implements Serializable {
    /**
     * 证书生效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date issuedTime = new Date();

    /**
     * 证书失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expiryTime = new Date(System.currentTimeMillis() + 1000*60*24*365);

    /**
     * 用户类型
     */
    private String consumerType = "user";

    /**
     * 用户数量
     */
    private Integer consumerAmount = 1;

    /**
     * 描述信息
     */
    private String description = "生成license";

    public static void main(String[] args) {
        System.out.println(System.currentTimeMillis());
    }


}
