package com.plasticene.boot.flow.core.impl.support;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;

/**
 * 校验宿主显式传入的流程操作者上下文。
 * @author ZFJ
 * @since 2026-11-11
 */
public final class FlowActors {
    private FlowActors() {
    }

    public static LoginUser require(LoginUser actor) {
        if (actor == null || actor.getId() == null || actor.getOrgId() == null) {
            throw new BizException("操作者及组织信息不能为空");
        }
        return actor;
    }
}
