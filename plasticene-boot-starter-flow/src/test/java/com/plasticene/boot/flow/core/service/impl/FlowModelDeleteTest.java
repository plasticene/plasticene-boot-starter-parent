package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证模型物理删除与发布版本标记删除的关联处理。
 * @author ZFJ
 * @since 2026-11-05
 */
@ExtendWith(MockitoExtension.class)
class FlowModelDeleteTest {
    private LoginUser loginUserContext;
    @Mock
    private FlowModelDAO models;
    @Mock
    private FlowDefinitionService definitions;
    @InjectMocks
    private FlowModelServiceImpl service;

    @BeforeEach
    void setup() {
        LoginUser user = new LoginUser();
        user.setOrgId(100L);
        user.setId(8L);
        loginUserContext = user;
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "delete"), FlowDefinition.class);
    }

    @Test
    void shouldMarkAllVersionsBeforePhysicallyDeletingModels() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(100L));
        when(definitions.update(any(Wrapper.class))).thenAnswer(invocation -> {
            LambdaUpdateWrapper<FlowDefinition> query = invocation.getArgument(0);
            assertTrue(query.getSqlSegment().contains("model_id"));
            assertTrue(query.getSqlSegment().contains("org_id"));
            assertTrue(query.getSqlSet().contains("is_delete="));
            assertTrue(query.getParamNameValuePairs().containsValue(11L));
            assertTrue(query.getParamNameValuePairs().containsValue(100L));
            assertTrue(query.getParamNameValuePairs().containsValue(1));
            return true;
        });
        service.delete(List.of(11L, 11L), loginUserContext);
        var order = inOrder(models, definitions);
        order.verify(models).selectFlowModelForUpdate(11L);
        order.verify(definitions).update(any(Wrapper.class));
        order.verify(models).deleteByIds(List.of(11L));
        verify(definitions, never()).remove(any(Wrapper.class));
    }

    @Test
    void shouldNotDeleteModelWhenVersionMarkingFails() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(100L));
        when(definitions.update(any(Wrapper.class))).thenThrow(new IllegalStateException("database failure"));
        assertThrows(IllegalStateException.class, () -> service.delete(List.of(11L), loginUserContext));
        verify(models, never()).deleteByIds(any());
    }

    @Test
    void shouldRejectOtherOrganizationsBeforeMarkingVersions() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(200L));
        assertThrows(BizException.class, () -> service.delete(List.of(11L), loginUserContext));
        verifyNoInteractions(definitions);
        verify(models, never()).deleteByIds(any());
    }

    @Test
    void shouldIgnoreEmptyOrMissingModels() {
        service.delete(List.of(), loginUserContext);
        service.delete(List.of(11L), loginUserContext);
        verifyNoInteractions(definitions);
        verify(models, never()).deleteByIds(any());
    }

    private FlowModel model(Long orgId) {
        FlowModel model = new FlowModel();
        model.setId(11L);
        model.setManagerUserIds(List.of(8L));
        model.setOrgId(orgId);
        return model;
    }
}
