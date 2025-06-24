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
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


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
    public EncryptTypeHandler<?> encryptTypeHandler() {
        return new EncryptTypeHandler<>();
    }

    @Bean
    @ConditionalOnMissingBean(EncryptService.class)
    public EncryptService encryptService() {
        Algorithm algorithm = encryptProperties.getAlgorithm();
        return switch (algorithm) {
            case BASE64 -> new Base64EncryptService();
            case AES -> new AESEncryptService();
        };
    }

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    public IdGenerator idGenerator() {
        return new IdGenerator(idProperties.getDatacenter(), idProperties.getWorker());
    }
}
