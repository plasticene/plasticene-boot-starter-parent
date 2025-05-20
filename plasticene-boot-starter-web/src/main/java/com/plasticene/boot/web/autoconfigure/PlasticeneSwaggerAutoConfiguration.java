package com.plasticene.boot.web.autoconfigure;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.plasticene.boot.web.core.prop.SwaggerProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 17:13
 */
@Configuration
@EnableKnife4j
@ConditionalOnProperty(prefix = "swagger-info", value = "enable", matchIfMissing = true)
@EnableConfigurationProperties(SwaggerProperties.class)
public class PlasticeneSwaggerAutoConfiguration {

//    /**
//     * 根据@Tag上的排序，写入x-order
//     *
//     * @return the global open api customizer
//     */
//    @Bean
//    public GlobalOpenApiCustomizer orderGlobalOpenApiCustomizer() {
//        return openApi -> {
//            if (openApi.getTags()!=null){
//                openApi.getTags().forEach(tag -> {
//                    Map<String,Object> map=new HashMap<>();
//                    map.put("x-order", RandomUtil.randomInt(0,100));
//                    tag.setExtensions(map);
//                });
//            }
//            if(openApi.getPaths()!=null){
//                openApi.addExtension("x-test123","333");
//                openApi.getPaths().addExtension("x-abb", RandomUtil.randomInt(1,100));
//            }
//
//        };
//    }

    @Bean
    public OpenAPI customOpenAPI(SwaggerProperties properties) {
        return new OpenAPI().info(apiInfo(properties));
    }

    /**
     * API 摘要信息
     */
    private Info apiInfo(SwaggerProperties properties) {
        return new Info()
                .title(properties.getTitle())
                .version(properties.getVersion())
                .description(properties.getDescription())
                .contact(new Contact()
                        .name(properties.getAuthor())
                        .url(properties.getUrl())
                        .email(properties.getEmail()));

    }


}
