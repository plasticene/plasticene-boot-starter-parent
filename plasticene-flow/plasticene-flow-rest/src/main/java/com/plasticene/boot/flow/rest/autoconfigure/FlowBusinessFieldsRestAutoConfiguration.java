package com.plasticene.boot.flow.rest.autoconfigure;

import com.plasticene.boot.flow.core.api.service.FlowBusinessFieldsApi;
import com.plasticene.boot.flow.rest.business.fields.controller.FlowCategoryFieldsController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * 在 HTTP 和业务字段均开启时注册分类字段管理接口。
 * @author ZFJ
 * @since 2026-11-20
 */
@AutoConfiguration(after = FlowRestAutoConfiguration.class,
        afterName = "com.plasticene.boot.flow.business.fields.autoconfigure.FlowBusinessFieldsAutoConfiguration")
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = {"ptc.flow.http.enabled", "ptc.flow.business-fields.enabled"}, havingValue = "true")
@ConditionalOnBean(FlowBusinessFieldsApi.class)
@Import(FlowCategoryFieldsController.class)
public class FlowBusinessFieldsRestAutoConfiguration {
}
