package com.plasticene.boot.web.autoconfigure;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.plasticene.boot.web.core.prop.SwaggerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 17:13
 */
@Configuration
@EnableSwagger2
@EnableKnife4j
@ConditionalOnClass({Docket.class, ApiInfoBuilder.class})
@ConditionalOnProperty(prefix = "swagger-info", value = "enable", matchIfMissing = true)
@EnableConfigurationProperties(SwaggerProperties.class)
public class PlasticeneSwaggerAutoConfiguration {


    @Bean
    public Docket createRestApi(SwaggerProperties properties) {
        // 创建 Docket 对象
        return new Docket(DocumentationType.SWAGGER_2)
                // 用来创建该 API 的基本信息，展示在文档的页面中（自定义展示的信息）
                .apiInfo(apiInfo(properties))
                // 设置扫描指定 package 包下的
                .select()
                .apis(basePackage(properties.getBasePackage()))
                .paths(PathSelectors.any())
                .build();
    }

    // ========== apiInfo ==========

    /**
     * API 摘要信息
     */
    private static ApiInfo apiInfo(SwaggerProperties properties) {
        return new ApiInfoBuilder()
                .title(properties.getTitle())
                .description(properties.getDescription())
                .contact(new Contact(properties.getAuthor(), properties.getUrl(), properties.getEmail()))
                .version(properties.getVersion())
                .build();
    }

}
