package com.plasticene.boot.license.core.param;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    private Date issuedTime=new Date();

    /**
     * 证书失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expiryTime;

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


    /**
     * 业务相关的额外信息，在生成证书安装之后可以基于该信息做逻辑处理
     */
    private Map<String, Object> extra = new HashMap<>();

    /**
     * 服务器系统信息
     */
    private SystemInfo systemInfo;



}
