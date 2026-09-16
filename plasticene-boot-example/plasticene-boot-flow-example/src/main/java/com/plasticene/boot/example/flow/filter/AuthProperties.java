package com.plasticene.boot.example.flow.filter;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ZFJ
 * @since 2026/9/16
 */
@Data
public class AuthProperties {
    /**
     * token过期时间, 单位分钟， 默认2小时
     */
    private Integer tokenExpireTime = 120;

    /**
     * 忽略认证的url
     */
    private List<String> skipUrls = new ArrayList<>();

    /**
     * 是否续期
     */
    private boolean renew = true;

    /**
     * 续期比例，默认是过期时间的0.5
     */
    private BigDecimal renewRate = BigDecimal.valueOf(0.5);
}

