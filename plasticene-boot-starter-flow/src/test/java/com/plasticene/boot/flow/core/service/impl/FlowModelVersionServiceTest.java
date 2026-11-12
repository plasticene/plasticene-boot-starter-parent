package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.dao.FlowDefinitionDAO;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.model.vo.FlowDefinitionVO;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import javax.sql.DataSource;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证模型历史快照读取和组织、模型归属隔离。
 * @author ZFJ
 * @since 2026-11-05
 */
@ExtendWith(MockitoExtension.class)
class FlowModelVersionServiceTest {
    private LoginUser loginUserContext;
    @Mock
    private FlowDefinitionDAO definitions;
    @Mock
    private FlowModelDAO models;
    @Mock
    private FlowOrganizationProvider organization;
    @Mock
    private FlowCategoryService flowCategoryService;
    @Mock
    private FlowInstanceDAO instances;
    @InjectMocks
    private FlowDefinitionServiceImpl service;

    @BeforeEach
    void setup() {
        LoginUser user = new LoginUser();
        user.setId(8L);
        user.setOrgId(100L);
        loginUserContext = user;
        for (Class<?> entity : List.of(FlowDefinition.class, FlowModel.class, FlowInstance.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "version"), entity);
        }
    }

    @Test
    void shouldListDescendingAndMarkOnlyActiveDefinitionWithoutChangingModel() {
        FlowModel model = model(100L);
        when(models.selectById(11L)).thenReturn(model);
        Mockito.lenient().when(models.selectFlowModelForUpdate(11L)).thenReturn(model);
        FlowDefinition latest = definition(31L, 3);
        FlowDefinition old = definition(21L, 2);
        when(definitions.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
            PtcLambdaQueryWrapper<FlowDefinition> query = invocation.getArgument(0);
            assertTrue(query.getSqlSegment().contains("ORDER BY version DESC"));
            assertTrue(query.getSqlSegment().contains("org_id"));
            assertTrue(query.getSqlSegment().contains("model_id"));
            assertTrue(query.getSqlSegment().contains("is_delete"));
            assertTrue(query.getParamNameValuePairs().containsValue(100L));
            assertTrue(query.getParamNameValuePairs().containsValue(11L));
            assertFalse(query.getSqlSelect().contains("model_node"));
            return List.of(latest, old);
        });
        when(organization.getUserMap(ArgumentMatchers.anyLong())).thenReturn(Map.of(8L, "张三"));

        List<FlowDefinitionVO> versions = service.listModelVersions(11L, loginUserContext);

        assertEquals(List.of(3, 2), versions.stream().map(v -> v.getVersion()).toList());
        assertTrue(versions.get(0).getActive());
        assertFalse(versions.get(1).getActive());
        assertEquals("张三", versions.get(0).getPublisherName());
        assertEquals(11L, versions.get(0).getModelId());
        assertNull(versions.get(0).getFormConfig());
        assertNull(versions.get(0).getModelNode());
        assertEquals(31L, model.getActiveDefinitionId());
        verify(models, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldPreviewOldOrDisabledVersionWithCompleteSnapshot() {
        FlowModel model = model(100L);
        model.setStatus(-1);
        when(models.selectById(11L)).thenReturn(model);
        Mockito.lenient().when(models.selectFlowModelForUpdate(11L)).thenReturn(model);
        FlowDefinition old = definition(21L, 2);
        old.setIcon("flow:expense");
        old.setStartUserType(3);
        old.setStartRoleIds(List.of(18L));
        old.setStartUserIds(List.of(8L));
        old.setStartDeptIds(List.of(9L));
        old.setManagerUserIds(List.of(8L));
        old.setFormConfig(Map.of("rule", List.of(Map.of("field", "amount", "type", "inputNumber"))));
        old.setModelNode(new FlowNode());
        when(definitions.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            PtcLambdaQueryWrapper<FlowDefinition> query = invocation.getArgument(0);
            assertTrue(query.getSqlSegment().contains("model_id"));
            assertTrue(query.getParamNameValuePairs().containsValue(21L));
            assertTrue(query.getParamNameValuePairs().containsValue(11L));
            assertTrue(query.getParamNameValuePairs().containsValue(100L));
            return old;
        });
        when(flowCategoryService.getCategoryMap(loginUserContext)).thenReturn(Map.of("finance", "财务"));

        FlowDefinitionVO snapshot = service.getModelVersion(11L, 21L, loginUserContext);

        assertEquals("flow:expense", snapshot.getIcon());
        assertEquals(21L, snapshot.getDefinitionId());
        assertEquals(11L, snapshot.getModelId());
        assertEquals("财务", snapshot.getCategoryName());
        assertEquals(old.getStartRoleIds(), snapshot.getStartRoleIds());
        assertEquals(old.getStartUserIds(), snapshot.getStartUserIds());
        assertEquals(old.getStartDeptIds(), snapshot.getStartDeptIds());
        assertEquals(old.getManagerUserIds(), snapshot.getManagerUserIds());
        assertEquals(old.getFormConfig(), snapshot.getFormConfig());
        assertNotNull(snapshot.getModelNode());
        assertNotNull(snapshot.getExtraSetting());
        verify(models, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectOtherOrganizationOrMissingModelBeforeReadingDefinitions() {
        when(models.selectById(11L)).thenReturn(model(200L));
        assertThrows(BizException.class, () -> service.listModelVersions(11L, loginUserContext));
        assertThrows(BizException.class, () -> service.getModelVersion(11L, 21L, loginUserContext));
        when(models.selectById(11L)).thenReturn(null);
        Mockito.lenient().when(models.selectFlowModelForUpdate(11L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.listModelVersions(11L, loginUserContext));
        verifyNoInteractions(definitions);
    }

    @Test
    void shouldRejectVersionOutsideModelOrOrganization() {
        when(models.selectById(11L)).thenReturn(model(100L));
        when(definitions.selectOne(any(Wrapper.class))).thenReturn(null);
        assertThrows(BizException.class, () -> service.getModelVersion(11L, 99L, loginUserContext));
        verifyNoInteractions(flowCategoryService);
    }

    @Test
    void shouldSaveEmptyFormObjectAndKeepPartialNodeUpdates() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        FlowModelParam omitted = mapper.readValue("{\"id\":11,\"modelNode\":{\"key\":\"start\",\"name\":\"发起人\",\"type\":0}}", FlowModelParam.class);
        FlowModelParam cleared = mapper.readValue("{\"id\":11,\"formConfig\":{}}", FlowModelParam.class);
        FlowModelServiceImpl modelService = new FlowModelServiceImpl();
        ReflectionTestUtils.setField(modelService, "flowModelDAO", models);
        when(models.updateById(any(FlowModel.class))).thenAnswer(invocation -> {
            FlowModel update = invocation.getArgument(0);
            if (update.getModelNode() != null) {
                assertNull(update.getFormConfig());
            } else {
                assertNotNull(update.getFormConfig());
                assertTrue(update.getFormConfig().isEmpty());
            }
            return 1;
        });

        when(models.selectFlowModelForUpdate(11L)).thenReturn(model(100L));
        modelService.update(omitted, loginUserContext);
        modelService.update(cleared, loginUserContext);
        verify(models, times(2)).updateById(any(FlowModel.class));
        verify(models, never()).update(any(FlowModel.class), any(Wrapper.class));
    }

    @Test
    void shouldRejectDeletedDefinitionForNewInstancesButKeepSnapshotReadable() {
        FlowModel model = model(100L);
        when(models.selectOne(any(Wrapper.class))).thenReturn(model);
        FlowDefinition deleted = definition(31L, 3);
        deleted.setIsDelete(1);
        ReflectionTestUtils.setField(service, "baseMapper", definitions);
        when(definitions.selectById(31L)).thenReturn(deleted);

        assertThrows(BizException.class, () -> service.getStartableDefinition(31L, loginUserContext));
        assertSame(deleted, service.getById(31L));
        assertFalse(TableInfoHelper.getTableInfo(FlowDefinition.class).isWithLogicDelete());
    }

    @Test
    void shouldUseTheValidatedSnapshotWithoutLockingOrRecheckingALaterModelChange() {
        FlowModel model = model(100L);
        model.setStatus(1);
        FlowDefinition published = definition(31L, 3);
        published.setStartUserType(0);
        published.setIsDelete(0);
        when(models.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            PtcLambdaQueryWrapper<FlowModel> query = invocation.getArgument(0);
            assertFalse(query.getSqlSegment().contains("FOR SHARE"));
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return model;
        });
        when(definitions.selectById(31L)).thenAnswer(invocation -> {
            // 模拟模型在有效状态读取之后被停用、切换版本；本次仍使用刚校验的发布快照。
            model.setStatus(-1);
            model.setActiveDefinitionId(41L);
            return published;
        });
        ReflectionTestUtils.setField(service, "baseMapper", definitions);
        var datasource = mock(DataSource.class);
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(datasource),
                new AnnotationTransactionAttributeSource()));
        var queries = (FlowDefinitionService) proxy.getProxy();

        assertSame(published, queries.getStartableDefinition(31L, loginUserContext));
        verify(models, times(1)).selectOne(any(Wrapper.class));
        verifyNoInteractions(datasource);
    }

    @Test
    void shouldUsePublishedIconInCatalogWhileDraftHasDifferentIcon() {
        FlowModel model = model(100L);
        model.setIcon("flow:form");
        model.setCategory("finance");
        FlowDefinition published = definition(31L, 3);
        published.setIsDelete(0);
        published.setStartUserType(0);
        published.setIcon("flow:expense");
        when(models.selectList(any(Wrapper.class))).thenReturn(List.of(model));
        when(flowCategoryService.getCategoryMap(loginUserContext)).thenReturn(Map.of("finance", "财务"));
        when(instances.selectList(any(Wrapper.class))).thenReturn(List.of());
        FlowDefinitionServiceImpl catalogService = spy(service);
        doReturn(List.of(published)).when(catalogService).listByIds(List.of(31L));

        var catalog = catalogService.getStartableCatalog(loginUserContext);

        assertEquals(1, catalog.getModels().size());
        assertEquals("flow:expense", catalog.getModels().get(0).getIcon());
        assertEquals("flow:form", model.getIcon());
    }

    private FlowModel model(Long orgId) {
        FlowModel model = new FlowModel();
        model.setId(11L);
        model.setOrgId(orgId);
        model.setManagerUserIds(List.of(8L));
        model.setActiveDefinitionId(31L);
        return model;
    }

    private FlowDefinition definition(Long id, int version) {
        FlowDefinition definition = new FlowDefinition();
        definition.setId(id);
        definition.setModelId(11L);
        definition.setOrgId(100L);
        definition.setVersion(version);
        definition.setCreator(8L);
        definition.setCategory("finance");
        definition.setPublishTime(LocalDateTime.of(2026, 11, 5, 10, 0));
        return definition;
    }
}
