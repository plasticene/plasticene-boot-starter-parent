package com.plasticene.boot.flow.core.api.spi;

import com.plasticene.boot.common.user.LoginUser;

/**
 * 由宿主授予流程事件运维权限，缺省禁止人工运维。
 * @author ZFJ
 * @since 2026-11-11
 */
@FunctionalInterface
public interface FlowEventManagementPolicy {
    boolean canManage(LoginUser actor);
}
