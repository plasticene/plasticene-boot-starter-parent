package com.plasticene.boot.mybatis.autoconfigure;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.plasticene.boot.mybatis.core.encrypt.AESEncryptService;
import com.plasticene.boot.mybatis.core.encrypt.Base64EncryptService;
import com.plasticene.boot.mybatis.core.encrypt.EncryptService;
import com.plasticene.boot.mybatis.core.enums.Algorithm;
import com.plasticene.boot.mybatis.core.handlers.DefaultDBFieldHandler;
import com.plasticene.boot.mybatis.core.handlers.EncryptTypeHandler;
import com.plasticene.boot.mybatis.core.prop.EncryptProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 13:55
 */
@Configuration
@EnableConfigurationProperties(EncryptProperties.class)
public class PlasticeneMybatisAutoConfiguration {
    @Resource
    private EncryptProperties encryptProperties;
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor()); // 分页插件
        return mybatisPlusInterceptor;
    }

    @Bean
    public MetaObjectHandler defaultMetaObjectHandler(){
        return new DefaultDBFieldHandler();
    }

    @Bean
    public EncryptTypeHandler encryptTypeHandler() {
        return new EncryptTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean(EncryptService.class)
    public EncryptService encryptService() {
        Algorithm algorithm = encryptProperties.getAlgorithm();
        EncryptService encryptService;
        switch (algorithm) {
            case BASE64:
                encryptService =  new Base64EncryptService();
                break;
            case AES:
                encryptService = new AESEncryptService();
                break;
            default:
                encryptService =  null;
        }
        return encryptService;
    }
}
