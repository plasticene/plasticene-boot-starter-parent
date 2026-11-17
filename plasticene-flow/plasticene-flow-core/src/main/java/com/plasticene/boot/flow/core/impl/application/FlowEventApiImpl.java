package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.service.FlowEventApi;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.spi.FlowResource;
import com.plasticene.boot.flow.core.api.vo.FlowEventFailureVO;
import com.plasticene.boot.flow.core.impl.event.FlowEventStore;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 实现 {@link FlowEventApi}，负责事件投递管理的授权与用例编排。
 * @author ZFJ
 * @since 2026-11-16
 */
@Service
public class FlowEventApiImpl implements FlowEventApi {
    @Resource
    private FlowAccess access;
    @Resource
    private FlowEventStore eventStore;

    @Override
    public List<FlowEventFailureVO> failures(int limit, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.EVENT_READ, access.organization(actor));
        return eventStore.failures(actor.getOrgId(), limit).stream().map(item -> new FlowEventFailureVO(item.eventId(), item.orgId(), item.instanceId(), item.category(), item.eventType(), item.status(), item.retryCount(), item.lastError(), item.nextAttemptAt())).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retry(String eventId, LoginUser actor) {
        FlowActors.require(actor);
        access.check(actor, FlowAction.EVENT_RETRY, new FlowResource(
                actor.getOrgId(), "EVENT", eventId, null, null, null, null, null, null));
        if (eventId == null || eventId.isBlank() || !eventStore.retry(actor.getOrgId(), eventId)) {
            throw new BizException("事件不存在或不处于暂停状态");
        }
    }
}
