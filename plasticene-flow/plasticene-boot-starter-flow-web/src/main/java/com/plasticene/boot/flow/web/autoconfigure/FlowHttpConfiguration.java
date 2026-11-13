package com.plasticene.boot.flow.web.autoconfigure;

import com.plasticene.boot.flow.autoconfigure.FlowAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 按需启用流程 HTTP 管理及运行时接口。
 * @author ZFJ
 * @since 2026-11-11
 */
@AutoConfiguration(after = FlowAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "ptc.flow.http", name = "enabled", havingValue = "true")
@ComponentScan("com.plasticene.boot.flow.web.controller")
public class FlowHttpConfiguration {
}
