package com.plasticene.boot.web.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotEmpty;

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
    @NotEmpty(message = "标题不能为空")
    private String title;
    /**
     * 描述
     */
    @NotEmpty(message = "描述不能为空")
    private String description;
    /**
     * 作者
     */
    @NotEmpty(message = "作者不能为空")
    private String author;
    /**
     * 版本
     */
    @NotEmpty(message = "版本不能为空")
    private String version;
    /**
     * 扫描的包
     */
    @NotEmpty(message = "扫描的 package 不能为空")
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
