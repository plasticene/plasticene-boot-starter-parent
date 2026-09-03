package com.plasticene.boot.flow.web;

import com.plasticene.boot.flow.autoconfigure.FlowProperties;
import com.plasticene.boot.flow.core.model.FlowActor;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从可配置的 HTTP 请求头读取租户、操作人和角色信息。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public class HeaderFlowWebActorResolver implements FlowWebActorResolver {

    private final FlowProperties.Web properties;

    public HeaderFlowWebActorResolver(FlowProperties.Web properties) {
        this.properties = properties;
    }

    @Override
    public FlowActor resolve(HttpServletRequest request) {
        String tenantId = requiredHeader(request, properties.getTenantHeader());
        String operatorId = requiredHeader(request, properties.getOperatorHeader());
        String rolesValue = request.getHeader(properties.getRolesHeader());
        Set<String> roles = rolesValue == null || rolesValue.isBlank()
                ? Set.of()
                : Arrays.stream(rolesValue.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new FlowActor(tenantId, operatorId, roles);
    }

    private String requiredHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required request header: " + headerName);
        }
        return value;
    }
}
