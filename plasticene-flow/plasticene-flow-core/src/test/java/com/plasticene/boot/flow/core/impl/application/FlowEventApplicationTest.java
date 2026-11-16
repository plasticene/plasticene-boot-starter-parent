package com.plasticene.boot.flow.core.impl.application;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.spi.FlowAction;
import com.plasticene.boot.flow.core.impl.event.FlowEventStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证事件运维必须经过统一授权并保持组织与暂停状态限制。
 * @author ZFJ
 * @since 2026-11-16
 */
class FlowEventApplicationTest {
    @Test
    void deniedRetryNeverReachesTheEventStore() {
        var application = new FlowEventApplication();
        var access = mock(FlowAccess.class); var store = mock(FlowEventStore.class);
        ReflectionTestUtils.setField(application, "access", access);
        ReflectionTestUtils.setField(application, "eventStore", store);
        var actor = new LoginUser(); actor.setId(1L); actor.setOrgId(9L);
        doThrow(new BizException("无权重投")).when(access).check(eq(actor), eq(FlowAction.EVENT_RETRY), any());
        assertThrows(BizException.class, () -> application.retry("event", actor));
        verifyNoInteractions(store);
    }
    @Test
    void retryUsesTrustedOrganizationAndRejectsNonPausedEvents() {
        var application = new FlowEventApplication();
        var access = mock(FlowAccess.class); var store = mock(FlowEventStore.class);
        ReflectionTestUtils.setField(application, "access", access);
        ReflectionTestUtils.setField(application, "eventStore", store);
        var actor = new LoginUser(); actor.setId(1L); actor.setOrgId(9L);
        when(store.retry(9L, "event")).thenReturn(false);
        assertThrows(BizException.class, () -> application.retry("event", actor));
        verify(store).retry(9L, "event");
        verify(access).check(eq(actor), eq(FlowAction.EVENT_RETRY), argThat(resource -> resource.orgId().equals(9L) && resource.resourceId().equals("event")));
    }
}
