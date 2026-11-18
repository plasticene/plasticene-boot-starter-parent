package com.plasticene.boot.example.flow.purchase;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.api.event.FlowEventConsumer;
import com.plasticene.boot.flow.core.api.event.FlowInstanceEvent;
import com.plasticene.boot.flow.core.model.enums.FlowInstanceStatusEnum;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 按采购分类接收实例事件，更新业务状态并在事务提交后模拟通知行政。
 * @author ZFJ
 * @since 2026-11-18
 */
@Component
public class PurchaseFlowEventConsumer implements FlowEventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseFlowEventConsumer.class);
    private final PurchaseApplyMapper mapper;

    public PurchaseFlowEventConsumer(PurchaseApplyMapper mapper) {
        this.mapper = mapper;
    }

    /** 与采购流程模型的分类编码保持一致。 */
    @Override
    public String businessType() {
        return PurchaseApplyService.CATEGORY;
    }

    /** 锁定业务单并幂等处理实例终态；异常继续抛出，交由 FLOW 重试。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(FlowEvent event) {
        if (!(event instanceof FlowInstanceEvent instance)) {
            return;
        }
        Integer target = switch (instance.eventType()) {
            case INSTANCE_APPROVED -> FlowInstanceStatusEnum.APPROVE.getCode();
            case INSTANCE_REJECTED -> FlowInstanceStatusEnum.REJECT.getCode();
            case INSTANCE_CANCELLED -> FlowInstanceStatusEnum.CANCEL.getCode();
            default -> null;
        };
        if (target == null) {
            return;
        }
        if (!Objects.equals(target, instance.status()) || instance.businessId() == null
                || !instance.businessId().matches("purchase:[1-9][0-9]*")) {
            throw new BizException("采购流程事件的状态或业务标识不正确");
        }
        Long id = Long.valueOf(instance.businessId().substring(PurchaseApplyService.BUSINESS_PREFIX.length()));
        PurchaseApply apply = mapper.find(instance.orgId(), id, true);
        if (apply == null || !Objects.equals(apply.getInstanceId(), instance.instanceId())) {
            throw new BizException("采购流程事件与业务单据不匹配");
        }
        if (!Objects.equals(apply.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())) {
            return;
        }
        if (mapper.finish(instance.orgId(), id, instance.instanceId(), target) != 1) {
            throw new BizException("采购申请状态更新失败");
        }
        if (Objects.equals(target, FlowInstanceStatusEnum.APPROVE.getCode())) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    LOG.info("【模拟通知行政】采购申请 {}「{}」已审批通过，金额 {} 元，请安排下单采购。",
                            apply.getId(), apply.getTitle(), apply.getAmount());
                }
            });
        }
    }
}
