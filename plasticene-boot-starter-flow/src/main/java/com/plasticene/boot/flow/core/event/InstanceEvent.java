package com.plasticene.boot.flow.core.event;

import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.enums.FlowInstanceEventTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

/**
 * 流程实例事件
 * @author ZFJ
 * @date 2025/9/8
 */
@Getter
@Setter
@ToString
public class InstanceEvent extends ApplicationEvent {
    /**
     * 当前审批流程实例
     */
    private FlowInstance instance;
    /**
     * 业务id
     */
    private Long businessId;

    /**
     * 流程分类 = 业务分类
     */
    private String category;
    /**
     * 当前节点信息
     */
    private FlowNode currentNode;

    /**
     * 事件类型
     */
    private FlowInstanceEventTypeEnum eventType;


    public InstanceEvent(Object source) {
        super(source);
    }
}
