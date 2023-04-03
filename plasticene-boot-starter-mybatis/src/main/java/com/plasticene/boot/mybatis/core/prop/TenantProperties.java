package com.plasticene.boot.mybatis.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Set;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/12/10 18:28
 */
@ConfigurationProperties(prefix = "ptc.tenant")
@Data
public class TenantProperties {


    /**
     * 是否开启多租户功能
     */
    private Boolean enable = Boolean.TRUE;

    /**
     * 需要忽略多租户的表
     *
     * 即默认所有表都开启多租户的功能，所以记得添加对应的 tenant_id 字段哟
     */
    private Set<String> ignoreTables = Collections.emptySet();

    /**
     * 需要忽略多租户的请求，例如登录接口这时候还不知道是哪家租户
     *
     * */
    private Set<String> ignoreUrls = Collections.emptySet();
}
