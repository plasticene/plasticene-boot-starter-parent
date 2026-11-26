package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.api.param.FlowBusinessFieldFilter;
import com.plasticene.boot.flow.core.api.service.FlowBusinessFieldsApi;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessFieldsLifecycle;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * 统一读取业务字段开关并提供可选实现。
 * @author ZFJ
 * @since 2026-11-20
 */
public class FlowBusinessFieldsSupportProvider implements SmartInitializingSingleton {

    private final boolean enabled;
    private final ObjectProvider<FlowBusinessFieldsApi> api;
    private final ObjectProvider<FlowBusinessFieldsLifecycle> lifecycle;

    public FlowBusinessFieldsSupportProvider(Environment environment, ObjectProvider<FlowBusinessFieldsApi> api,
                                             ObjectProvider<FlowBusinessFieldsLifecycle> lifecycle) {
        this.enabled = environment.getProperty("ptc.flow.business-fields.enabled", Boolean.class, false);
        this.api = api;
        this.lifecycle = lifecycle;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (enabled && (api.getIfAvailable() == null || lifecycle.getIfAvailable() == null)) {
            throw new IllegalStateException("已启用审批业务字段，请引入 plasticene-flow-business-fields");
        }
    }

    public FlowBusinessFieldsApi api() {
        return enabled ? api.getObject() : null;
    }

    public FlowBusinessFieldsLifecycle lifecycle() {
        return enabled ? lifecycle.getObject() : null;
    }

    public FlowBusinessFieldsVO.Capabilities capabilities() {
        return new FlowBusinessFieldsVO.Capabilities(enabled, enabled ? api().limits() : null);
    }

    public static void requireNoFilters(List<FlowBusinessFieldFilter> filters) {
        if (filters != null && !filters.isEmpty()) {
            throw new BizException("当前查询不支持业务字段筛选");
        }
    }
}
