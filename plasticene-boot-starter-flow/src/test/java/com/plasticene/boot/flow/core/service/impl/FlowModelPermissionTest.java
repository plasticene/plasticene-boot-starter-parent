package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.model.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.validator.FlowNodeValidator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证流程模型只读访问及管理员写入权限。
 * @author ZFJ
 * @since 2026-11-09
 */
@ExtendWith(MockitoExtension.class)
class FlowModelPermissionTest {
    @Mock
    private FlowModelDAO models;
    @Mock
    private FlowDefinitionService definitions;
    @Mock
    private FlowNodeValidator validator;
    @InjectMocks
    private FlowModelServiceImpl service;

    @BeforeEach
    void setup() {
        LoginUser user = new LoginUser();
        user.setId(8L);
        user.setOrgId(100L);
        LoginUserHolder.set(user);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "permission"), FlowModel.class);
    }

    @AfterEach
    void cleanup() {
        LoginUserHolder.remove();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1})
    void shouldAllowNonManagerToReadCompleteDraftInAnyStatus(int status) {
        FlowModel model = model(11L, 100L, List.of(9L));
        model.setStatus(status);
        model.setFormConfig(java.util.Map.of("rule", List.of(java.util.Map.of("field", "reason"))));
        when(models.selectById(11L)).thenReturn(model);

        var detail = service.detail(11L);

        assertEquals(status, detail.getStatus());
        assertEquals(model.getFormConfig(), detail.getFormConfig());
        verify(models, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectMissingAndOtherOrganizationDetails() {
        when(models.selectById(11L)).thenReturn(null, model(11L, 200L, List.of(8L)));
        assertThrows(BizException.class, () -> service.detail(11L));
        assertThrows(BizException.class, () -> service.detail(11L));
    }

    @Test
    void shouldRejectForgedManagerListBeforeUpdating() {
        when(models.selectById(11L)).thenReturn(model(11L, 100L, List.of(9L)));
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        param.setManagerUserIds(List.of(8L));

        assertThrows(BizException.class, () -> service.update(param));
        verify(models, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldAllowOriginalManagerToChangeManagerList() {
        when(models.selectById(11L)).thenReturn(model(11L, 100L, List.of(8L)));
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        param.setManagerUserIds(List.of(9L));
        service.update(param);
        verify(models).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectCrossOrganizationWritesEvenWhenUserIsListedAsManager() {
        FlowModel model = model(11L, 200L, List.of(8L));
        when(models.selectById(11L)).thenReturn(model);
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model);
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        assertThrows(BizException.class, () -> service.update(param));
        assertThrows(BizException.class, () -> service.enable(enableParam(1)));
        assertThrows(BizException.class, () -> service.publish(11L));
        assertThrows(BizException.class, () -> service.delete(List.of(11L)));
        verifyNoInteractions(definitions, validator);
        verify(models, never()).updateById(any(FlowModel.class));
        verify(models, never()).update(any(Wrapper.class));
        verify(models, never()).deleteByIds(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void shouldRejectNonManagerStatusChange(int status) {
        when(models.selectById(11L)).thenReturn(model(11L, 100L, List.of(9L)));
        assertThrows(BizException.class, () -> service.enable(enableParam(status)));
        verify(models, never()).update(any(Wrapper.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void shouldAllowManagerStatusChange(int status) {
        FlowModel model = model(11L, 100L, List.of(8L));
        model.setStatus(status == 1 ? -1 : 1);
        when(models.selectById(11L)).thenReturn(model);
        service.enable(enableParam(status));
        verify(models).update(any(Wrapper.class));
    }

    @Test
    void shouldRejectNonManagerPublishBeforeCreatingSnapshot() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(11L, 100L, List.of(9L)));
        assertThrows(BizException.class, () -> service.publish(11L));
        verifyNoInteractions(definitions, validator);
        verify(models, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectEntireDeleteBatchBeforeAnyWrite() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(11L, 100L, List.of(8L)));
        when(models.selectFlowModelForUpdate(12L)).thenReturn(model(12L, 100L, List.of(9L)));
        assertThrows(BizException.class, () -> service.delete(List.of(11L, 12L)));
        verifyNoInteractions(definitions);
        verify(models, never()).deleteByIds(any());
    }

    private FlowModel model(Long id, Long orgId, List<Long> managers) {
        FlowModel model = new FlowModel();
        model.setId(id);
        model.setOrgId(orgId);
        model.setManagerUserIds(managers);
        return model;
    }

    private FlowModelEnableParam enableParam(int status) {
        FlowModelEnableParam param = new FlowModelEnableParam();
        param.setId(11L);
        param.setStatus(status);
        return param;
    }
}
