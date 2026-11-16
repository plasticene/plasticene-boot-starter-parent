package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;
import com.plasticene.boot.flow.core.api.service.*;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.api.validation.group.*;
import jakarta.validation.groups.Default;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.plasticene.boot.flow.core.impl.event.FlowEventStore;

/**
 * 实现FlowEventOperations的统一授权与操作编排。
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
        access.check(actor, FlowAction.EVENT_RETRY, new com.plasticene.boot.flow.core.api.spi.FlowResource(
                actor.getOrgId(), "EVENT", eventId, null, null, null, null, null, null));
        if (eventId == null || eventId.isBlank() || !eventStore.retry(actor.getOrgId(), eventId)) {
            throw new com.plasticene.boot.common.exception.BizException("事件不存在或不处于暂停状态");
        }
    }
}
