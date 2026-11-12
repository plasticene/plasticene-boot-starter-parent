package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.model.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.validator.FlowNodeValidator;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    private LoginUser loginUserContext;
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
        loginUserContext = user;
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "permission"), FlowModel.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1})
    void shouldAllowNonManagerToReadCompleteDraftInAnyStatus(int status) {
        FlowModel model = model(11L, 100L, List.of(9L));
        model.setStatus(status);
        model.setFormConfig(Map.of("rule", List.of(Map.of("field", "reason"))));
        when(models.selectById(11L)).thenReturn(model);
        Mockito.lenient().when(models.selectFlowModelForUpdate(11L)).thenReturn(model);

        var detail = service.detail(11L, loginUserContext);

        assertEquals(status, detail.getStatus());
        assertEquals(model.getFormConfig(), detail.getFormConfig());
        verify(models, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectMissingAndOtherOrganizationDetails() {
        when(models.selectById(11L)).thenReturn(null, model(11L, 200L, List.of(8L)));
        assertThrows(BizException.class, () -> service.detail(11L, loginUserContext));
        assertThrows(BizException.class, () -> service.detail(11L, loginUserContext));
    }

    @Test
    void shouldRejectForgedManagerListBeforeUpdating() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(11L, 100L, List.of(9L)));
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        param.setManagerUserIds(List.of(8L));

        assertThrows(BizException.class, () -> service.update(param, loginUserContext));
        verify(models, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldAllowOriginalManagerToChangeManagerList() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(11L, 100L, List.of(8L)));
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        param.setManagerUserIds(List.of(9L));
        service.update(param, loginUserContext);
        verify(models).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectCrossOrganizationWritesEvenWhenUserIsListedAsManager() {
        FlowModel model = model(11L, 200L, List.of(8L));
        when(models.selectById(11L)).thenReturn(model);
        Mockito.lenient().when(models.selectFlowModelForUpdate(11L)).thenReturn(model);
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model);
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        assertThrows(BizException.class, () -> service.update(param, loginUserContext));
        assertThrows(BizException.class, () -> service.enable(enableParam(1), loginUserContext));
        assertThrows(BizException.class, () -> service.publish(11L, loginUserContext));
        assertThrows(BizException.class, () -> service.delete(List.of(11L), loginUserContext));
        verifyNoInteractions(definitions, validator);
        verify(models, never()).updateById(any(FlowModel.class));
        verify(models, never()).update(any(Wrapper.class));
        verify(models, never()).deleteByIds(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void shouldRejectNonManagerStatusChange(int status) {
        when(models.selectById(11L)).thenReturn(model(11L, 100L, List.of(9L)));
        assertThrows(BizException.class, () -> service.enable(enableParam(status), loginUserContext));
        verify(models, never()).update(any(Wrapper.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void shouldAllowManagerStatusChange(int status) {
        FlowModel model = model(11L, 100L, List.of(8L));
        model.setStatus(status == 1 ? -1 : 1);
        when(models.selectById(11L)).thenReturn(model);
        Mockito.lenient().when(models.selectFlowModelForUpdate(11L)).thenReturn(model);
        service.enable(enableParam(status), loginUserContext);
        verify(models).update(any(Wrapper.class));
    }

    @Test
    void shouldRejectNonManagerPublishBeforeCreatingSnapshot() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(11L, 100L, List.of(9L)));
        assertThrows(BizException.class, () -> service.publish(11L, loginUserContext));
        verifyNoInteractions(definitions, validator);
        verify(models, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectEntireDeleteBatchBeforeAnyWrite() {
        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(11L, 100L, List.of(8L)));
        when(models.selectFlowModelForUpdate(12L)).thenReturn(model(12L, 100L, List.of(9L)));
        assertThrows(BizException.class, () -> service.delete(List.of(11L, 12L), loginUserContext));
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
