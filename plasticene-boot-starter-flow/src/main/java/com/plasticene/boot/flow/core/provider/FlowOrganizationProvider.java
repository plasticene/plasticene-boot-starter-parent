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

    default Map<Long, String> getDeptMap() {
        return Map.of();
    }

    default Map<Long, String> getRoleMap() {
        return Map.of();
    }
}
