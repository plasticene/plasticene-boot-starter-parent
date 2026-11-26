package com.plasticene.boot.flow.business.fields.autoconfigure;

import com.plasticene.boot.flow.business.fields.application.FlowBusinessFieldsApiImpl;
import com.plasticene.boot.flow.business.fields.application.FlowBusinessFieldsLifecycleImpl;
import com.plasticene.boot.flow.business.fields.config.FlowBusinessFieldsProperties;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowBusinessFieldsQueryDAO;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowCategoryFieldDAO;
import com.plasticene.boot.flow.business.fields.persistence.dao.FlowInstanceFieldValueDAO;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsCategoryService;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsFieldResolver;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsQuerySupport;
import com.plasticene.boot.flow.business.fields.service.FlowBusinessFieldsValueService;
import com.plasticene.boot.flow.core.impl.application.FlowAccess;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 只在接入方明确开启时装配审批业务字段服务和映射。
 * @author ZFJ
 * @since 2026-11-20
 */
@AutoConfiguration(afterName = "com.plasticene.boot.flow.autoconfigure.FlowAutoConfiguration")
@ConditionalOnBean(FlowAccess.class)
@ConditionalOnProperty(prefix = "ptc.flow.business-fields", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FlowBusinessFieldsProperties.class)
@MapperScan(basePackageClasses = FlowBusinessFieldsQueryDAO.class)
@Import({FlowBusinessFieldsApiImpl.class, FlowBusinessFieldsLifecycleImpl.class,
        FlowBusinessFieldsFieldResolver.class, FlowBusinessFieldsCategoryService.class,
        FlowBusinessFieldsValueService.class, FlowBusinessFieldsQuerySupport.class})
public class FlowBusinessFieldsAutoConfiguration {

    @Bean
    public SmartInitializingSingleton flowBusinessFieldsSchemaCheck(FlowBusinessFieldsProperties properties,
            FlowCategoryFieldDAO categoryFields, FlowInstanceFieldValueDAO fieldValues) {
        return () -> {
            if (properties.getMaxDisplayFields() < 1 || properties.getMaxSearchFields() < 1) {
                throw new IllegalStateException("审批列表字段数量上限必须为正整数");
            }
            try {
                categoryFields.checkSchema();
                fieldValues.checkSchema();
            } catch (RuntimeException exception) {
                throw new IllegalStateException("审批业务字段扩展表不可用，请检查数据库及 flow-approval-list-fields.sql 迁移",
                        exception);
            }
        };
    }
}
