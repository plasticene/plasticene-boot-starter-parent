package com.plasticene.boot.flow.core.api.spi;

import java.util.Map;

/**
 * @author ZFJ
 * @since 2026/4/14
 */
public interface FlowOrganizationProvider {

    default Map<Long, String> getUserMap(Long orgId) {
            return Map.of();
        }

    default Map<Long, String> getActiveUserMap(Long orgId) {
        return Map.of();
    }

    default Map<Long, String> getDeptMap(Long orgId) {
        return Map.of();
    }

    default Map<Long, String> getRoleMap(Long orgId) {
        return Map.of();
    }
}
