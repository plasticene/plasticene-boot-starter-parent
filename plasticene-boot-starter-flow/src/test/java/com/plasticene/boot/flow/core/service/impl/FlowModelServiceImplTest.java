package com.plasticene.boot.flow.core.service.impl;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.enums.FlowModelEnums;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程发起权限判断测试
 *
 * @author ZFJ
 * @since 2026-09-14
 */
class FlowModelServiceImplTest {

    private final FlowDefinitionServiceImpl service = new FlowDefinitionServiceImpl();

    @Test
    void shouldAllowAllUsers() {
        FlowDefinition definition = definition(FlowModelEnums.StartUserType.ALL);

        assertTrue(service.canStart(definition, loginUser()));
    }

    @Test
    void shouldMatchSpecifiedUserAndDepartment() {
        LoginUser loginUser = loginUser();
        FlowDefinition userDefinition = definition(FlowModelEnums.StartUserType.USER);
        userDefinition.setStartUserIds(List.of(6L, 8L));
        FlowDefinition deptDefinition = definition(FlowModelEnums.StartUserType.DEPT);
        deptDefinition.setStartDeptIds(List.of(3L));

        assertTrue(service.canStart(userDefinition, loginUser));
        assertTrue(service.canStart(deptDefinition, loginUser));
    }

    @Test
    void shouldMatchAnySpecifiedRole() {
        FlowDefinition definition = definition(FlowModelEnums.StartUserType.ROLE);
        definition.setStartRoleIds(List.of(12L, 18L));

        assertTrue(service.canStart(definition, loginUser()));
    }

    @Test
    void shouldRejectMissingScopeMatch() {
        FlowDefinition definition = definition(FlowModelEnums.StartUserType.ROLE);
        definition.setStartRoleIds(List.of(99L));

        assertFalse(service.canStart(definition, loginUser()));
    }

    private FlowDefinition definition(FlowModelEnums.StartUserType startUserType) {
        FlowDefinition definition = new FlowDefinition();
        definition.setStartUserType(startUserType.getCode());
        return definition;
    }

    private LoginUser loginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(8L);
        loginUser.setDeptId(3L);
        loginUser.setRoleIds(List.of(18L, 20L));
        return loginUser;
    }
}
