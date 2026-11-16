package com.plasticene.boot.flow.core.api.spi;

import com.plasticene.boot.common.user.LoginUser;

/**
 * 由宿主提供流程操作的功能授权。
 * @author ZFJ
 * @since 2026-11-16
 */
@FunctionalInterface
public interface FlowAuthorizationPolicy {
    boolean isAllowed(LoginUser actor, FlowAction action, FlowResource resource);
}
