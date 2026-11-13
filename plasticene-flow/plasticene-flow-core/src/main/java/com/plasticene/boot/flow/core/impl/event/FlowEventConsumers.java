package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.api.event.FlowEventConsumer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 按不区分大小写的分类编码注册唯一事件接收方。
 * @author ZFJ
 * @since 2026-11-11
 */
public class FlowEventConsumers {
    private final Map<String, FlowEventConsumer> consumers = new HashMap<>();

    public FlowEventConsumers(Collection<FlowEventConsumer> registrations) {
        for (FlowEventConsumer consumer : registrations) {
            String key = normalize(consumer.businessType());
            if (consumers.putIfAbsent(key, consumer) != null) {
                throw new IllegalArgumentException("流程分类存在重复事件接收方: " + key);
            }
        }
    }

    public static String normalize(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("流程分类编码不能为空");
        }
        return category.toLowerCase(Locale.ROOT);
    }

    public void deliver(FlowEvent event) throws Exception {
        FlowEventConsumer consumer = consumers.get(normalize(event.category()));
        if (consumer == null) {
            throw new IllegalStateException("未注册流程分类事件接收方: " + event.category());
        }
        consumer.receive(event);
    }
}
