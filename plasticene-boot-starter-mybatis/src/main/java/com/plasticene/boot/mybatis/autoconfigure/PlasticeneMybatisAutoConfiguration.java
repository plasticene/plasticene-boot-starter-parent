package com.plasticene.boot.mybatis.autoconfigure;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.plasticene.boot.common.utils.IdGenerator;
import com.plasticene.boot.mybatis.core.encrypt.AESEncryptService;
import com.plasticene.boot.mybatis.core.encrypt.Base64EncryptService;
import com.plasticene.boot.mybatis.core.encrypt.EncryptService;
import com.plasticene.boot.mybatis.core.enums.Algorithm;
import com.plasticene.boot.mybatis.core.handlers.DefaultDBFieldHandler;
import com.plasticene.boot.mybatis.core.handlers.EncryptTypeHandler;
import com.plasticene.boot.mybatis.core.handlers.TenantDatabaseHandler;
import com.plasticene.boot.mybatis.core.prop.EncryptProperties;
import com.plasticene.boot.mybatis.core.prop.IdProperties;
import com.plasticene.boot.mybatis.core.prop.TenantProperties;
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
@EnableConfigurationProperties({EncryptProperties.class, IdProperties.class, TenantProperties.class})
public class PlasticeneMybatisAutoConfiguration {
    @Resource
    private EncryptProperties encryptProperties;
    @Resource
    private IdProperties idProperties;


    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantProperties properties) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        // 必须保证多租户插件在分页插件之前，这个是 MyBatis-plus 的规定
        if (properties.getEnable()) {
            mybatisPlusInterceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantDatabaseHandler(properties)));
        }
        // 分页插件
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
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

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public IdGenerator idGenerator() {
        IdGenerator idGenerator = new IdGenerator(idProperties.getDatacenter(), idProperties.getWorker());
        return idGenerator;
    }
}
