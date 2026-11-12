package com.plasticene.boot.flow.core.provider;

import java.util.Map;

/**
 * @author ZFJ
 * @since 2026/4/14
 */
public interface FlowOrganizationProvider {
    default Map<Long, String> getUserMap() {
        return Map.of();
    }

    /**
     * 当前登录用户所属组织内启用用户；供转交校验使用，未接入时默认禁止转交。
     * 实现方必须按组织隔离并过滤禁用、删除用户。
     */
    default Map<Long, String> getActiveUserMap() {
        return Map.of();
    }

    default Map<Long, String> getDeptMap() {
        return Map.of();
    }

    default Map<Long, String> getRoleMap() {
        return Map.of();
    }

    default Map<Long, String> getUserMap(Long orgId) {
        return getUserMap();
    }

    default Map<Long, String> getActiveUserMap(Long orgId) {
        return getActiveUserMap();
    }

    default Map<Long, String> getDeptMap(Long orgId) {
        return getDeptMap();
    }

    default Map<Long, String> getRoleMap(Long orgId) {
        return getRoleMap();
    }
}
