package com.plasticene.boot.flow.core.event;

import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

import java.util.Objects;

/**
 * 流程实例事件监听器抽象
 * 流程实例状态变更会发布事件，这时候监听实例事件的监听器都会收到事件通知
 * 不同的业务发起审批，生成流程实例，这样流程实例事件会通知到所有的业务监听器
 * 但是只需要该流程实例对应的业务监听器处理事件，其他业务的监听器不做任何处理
 * @author ZFJ
 * @date 2025/9/8
 */
public abstract class BaseFlowInstanceListener implements ApplicationListener<InstanceEvent> {

    @Override
    public void onApplicationEvent(@NonNull InstanceEvent event) {
        String category = event.getCategory();
        String businessType = businessType();
        // 判断当前业务类型和实例事件的业务是否一致
        if (!Objects.equals(businessType, category)) {
            return;
        }
        // 真正处理实例事件
        doProcessInstanceEvent(event);
    }

    /**
     * 处理事件
     * @param event 事件
     */
    public abstract void doProcessInstanceEvent(InstanceEvent event);

    /**
     * 监听器业务类型  要和流程模型类型一致
     * @return 业务类型
     */
    public abstract String businessType();
}
