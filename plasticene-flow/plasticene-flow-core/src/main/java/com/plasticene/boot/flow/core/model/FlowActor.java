package com.plasticene.boot.flow.core.model;

import java.util.Set;

/**
 * 审批流操作人及其租户、角色信息。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public record FlowActor(String tenantId, String operatorId, Set<String> roles) {

    public FlowActor {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (operatorId == null || operatorId.isBlank()) {
            throw new IllegalArgumentException("operatorId must not be blank");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
