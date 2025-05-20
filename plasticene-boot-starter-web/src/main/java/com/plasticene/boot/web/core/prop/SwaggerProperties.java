package com.plasticene.boot.web.core.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 17:08
 */
@ConfigurationProperties("swagger-info")
@Data
public class SwaggerProperties {
    /**
     * 标题
     */
    private String title;
    /**
     * 描述
     */
    private String description;
    /**
     * 作者
     */
    private String author;
    /**
     * 版本
     */
    private String version;
    /**
     * 扫描的包
     */
    private String basePackage;

    /**
     * url
     */
    private String url;

    /**
     * 邮箱
     */
    private String email;


}
