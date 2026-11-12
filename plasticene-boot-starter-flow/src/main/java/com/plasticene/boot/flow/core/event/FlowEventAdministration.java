package com.plasticene.boot.flow.core.event;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.utils.FlowActors;
import java.util.List;

/**
 * 在组织和运维权限范围内查询失败事件及人工重新投递。
 * @author ZFJ
 * @since 2026-11-11
 */
public class FlowEventAdministration {
    private final FlowEventStore store;
    private final FlowEventManagementPolicy policy;

    public FlowEventAdministration(FlowEventStore store, FlowEventManagementPolicy policy) {
        this.store = store;
        this.policy = policy;
    }

    public List<FlowEventDelivery> failures(LoginUser actor, int limit) {
        requireManager(actor);
        return store.failures(actor.getOrgId(), limit);
    }

    public void retry(String eventId, LoginUser actor) {
        requireManager(actor);
        if (eventId == null || !store.retry(actor.getOrgId(), eventId)) {
            throw new BizException("事件不存在或不处于暂停状态");
        }
    }

    private void requireManager(LoginUser actor) {
        FlowActors.require(actor);
        if (!policy.canManage(actor)) {
            throw new BizException("无流程事件运维权限");
        }
    }
}
