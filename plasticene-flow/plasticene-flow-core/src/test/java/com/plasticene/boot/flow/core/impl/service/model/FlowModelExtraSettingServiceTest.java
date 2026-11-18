package com.plasticene.boot.flow.core.impl.service.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowCategoryDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowCategory;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.flow.core.model.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.FlowFieldPermission;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.model.FlowTimeoutRule;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import com.plasticene.boot.flow.core.api.param.FlowModelParam;
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryService;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.validation.FlowNodeValidator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.plasticene.boot.flow.core.impl.application.FlowVariables;
import com.plasticene.boot.flow.core.api.spi.FlowBusinessVariableProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 流程模型更多设置发布测试
 *
 * @author ZFJ
 * @since 2026-09-28
 */
@ExtendWith(MockitoExtension.class)
class FlowModelExtraSettingServiceTest {
    private LoginUser loginUserContext;

    @Mock
    private FlowModelDAO flowModelDAO;
    @Mock
    private FlowCategoryService flowCategoryService;
    @Mock
    private FlowNodeValidator flowNodeValidator;
    @Mock
    private FlowDefinitionService flowDefinitionService;
    @Spy
    private FlowVariables flowVariables = new FlowVariables();
    @InjectMocks
    private FlowModelServiceImpl service;

    @Mock
    private FlowCategoryDAO flowCategoryDAO;

    @BeforeEach
    void setupUser() {
        Mockito.lenient().when(flowCategoryDAO.selectByCodeForUpdate(
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString()))
                .thenReturn(new FlowCategory());

        var user = new LoginUser();
        user.setId(8L);
        user.setOrgId(100L);
        loginUserContext = user;
        var model = new FlowModel();
        {
        model.setFormMode(0);
        }
        model.setCategory("leave");
        model.setId(11L);
        model.setOrgId(100L);
        model.setManagerUserIds(List.of(8L));
        Mockito.lenient().when(flowModelDAO.selectById(11L)).thenReturn(model);
        Mockito.lenient().when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(model);
    }

    @Test
    void shouldRejectInvalidDefaultTimeoutWhenCreating() {
        FlowModelParam param = timeoutParam();
        param.getExtraSetting().getTimeoutRule().setAutoEnabled(true);

        assertThrows(BizException.class, () -> service.create(param, loginUserContext));
        verifyNoInteractions(flowModelDAO);
    }

    @Test
    void shouldRejectInvalidDefaultTimeoutWhenUpdating() {
        FlowModelParam param = timeoutParam();
        param.getExtraSetting().getTimeoutRule().setReminderEnabled(true);

        assertThrows(BizException.class, () -> service.update(param, loginUserContext));
        verifyNoInteractions(flowModelDAO);
    }

    @Test
    void shouldSaveValidDefaultTimeoutWithoutValidatingDraftNodes() {
        FlowModelParam param = timeoutParam();
        FlowTimeoutRule rule = param.getExtraSetting().getTimeoutRule();
        rule.setAutoEnabled(true);
        rule.setAutoAfter(2);
        rule.setAction(FlowTimeoutRule.Action.APPROVE);
        FlowNode draft = new FlowNode();
        draft.setTimeoutMode(FlowTimeoutRule.Mode.CUSTOM);
        param.setModelNode(draft);

        service.update(param, loginUserContext);

        ArgumentCaptor<FlowModel> captor = ArgumentCaptor.forClass(FlowModel.class);
        verify(flowModelDAO).updateById(captor.capture());
        assertEquals(2, captor.getValue().getExtraSetting().getTimeoutRule().getAutoAfter());
        verifyNoInteractions(flowNodeValidator);
    }

    @Test
    void shouldKeepExtraSettingAbsentWhenUpdatingOnlyNodes() {
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        param.setModelNode(new FlowNode());

        service.update(param, loginUserContext);

        ArgumentCaptor<FlowModel> captor = ArgumentCaptor.forClass(FlowModel.class);
        verify(flowModelDAO).updateById(captor.capture());
        assertNull(captor.getValue().getExtraSetting());
    }

    @Test
    void shouldCopyExtraSettingToPublishedDefinition() {
        FlowModel model = new FlowModel();
        {
        model.setFormMode(0);
        }
        model.setCategory("leave");
        model.setId(11L);
        model.setIcon("flow:expense");
        model.setModelNode(new FlowNode());
        FlowExtraSetting setting = new FlowExtraSetting();
        setting.setAllowSubmitterCancel(false);
        setting.setAllowApproverWithdraw(true);
        setting.setApproverDeduplication(FlowExtraSetting.ApproverDeduplication.ONCE_PER_PROCESS);
        model.setExtraSetting(setting);
        when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(model);
        when(flowDefinitionService.getMaxVersion(11L)).thenReturn(2);

        model.setOrgId(100L);
        model.setManagerUserIds(List.of(8L));
        service.publish(11L, loginUserContext);
        verify(flowNodeValidator).validate(model.getModelNode(), new FlowVariableSchema());

        ArgumentCaptor<FlowDefinition> captor = ArgumentCaptor.forClass(FlowDefinition.class);
        verify(flowDefinitionService).save(captor.capture());
        FlowExtraSetting snapshot = captor.getValue().getExtraSetting();
        assertEquals(false, snapshot.getAllowSubmitterCancel());
        assertEquals(true, snapshot.getAllowApproverWithdraw());
        assertEquals(FlowExtraSetting.ApproverDeduplication.ONCE_PER_PROCESS,
                snapshot.getApproverDeduplication());
        assertEquals(3, captor.getValue().getVersion());
        assertEquals("flow:expense", captor.getValue().getIcon());
    }

    @Test
    void shouldPreserveNodeOperationPermissionsThroughDraftAndPublishedJson() throws Exception {
        FlowNode root = new FlowNode();
        root.setKey("start");
        FlowNode approval = new FlowNode();
        approval.setKey("approve");
        approval.setAllowTransfer(false);
        approval.setAllowReturn(true);
        approval.setAllowAddSign(false);
        var fieldPermission = new FlowFieldPermission();
        fieldPermission.setField("reason");
        fieldPermission.setTitle("原因");
        fieldPermission.setPermission(FlowFieldPermission.Permission.WRITE);
        approval.setFieldsPermission(List.of(fieldPermission));
        root.setChildNode(approval);
        var mapper = new ObjectMapper();
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        param.setFormMode(0);
        param.setModelNode(mapper.readValue(mapper.writeValueAsString(root), FlowNode.class));
        service.update(param, loginUserContext);
        ArgumentCaptor<FlowModel> draft = ArgumentCaptor.forClass(FlowModel.class);
        verify(flowModelDAO).updateById(draft.capture());
        when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(draft.getValue());

        draft.getValue().setCategory("leave");
        draft.getValue().setOrgId(100L);
        draft.getValue().setManagerUserIds(List.of(8L));
        service.publish(11L, loginUserContext);

        ArgumentCaptor<FlowDefinition> published = ArgumentCaptor.forClass(FlowDefinition.class);
        verify(flowDefinitionService).save(published.capture());
        FlowNode restored = mapper.readValue(mapper.writeValueAsString(published.getValue().getModelNode()), FlowNode.class);
        assertEquals(false, restored.getChildNode().getAllowTransfer());
        assertEquals(true, restored.getChildNode().getAllowReturn());
        assertEquals(false, restored.getChildNode().getAllowAddSign());
        assertEquals(FlowFieldPermission.Permission.WRITE, restored.getChildNode().getFieldsPermission().getFirst().getPermission());
    }

    @Test
    void shouldRejectInvalidFormOnSaveWithoutTraversingDraftNodes() {
        var param = new FlowModelParam();
        param.setFormConfig(Map.of("rule", List.of(
                Map.of("type", "input", "field", "name", "title", ""))));
        assertThrows(BizException.class, () -> service.create(param, loginUserContext));
        assertThrows(BizException.class, () -> service.update(param, loginUserContext));
        verifyNoInteractions(flowModelDAO, flowNodeValidator);
    }

    @Test
    void shouldPassEligibleFieldsFromValidatedPublishedForm() {
        var model = new FlowModel();
        {
        model.setFormMode(0);
        }
        model.setCategory("leave");
        model.setModelNode(new FlowNode());
        model.setFormConfig(Map.of("rule", List.of(
                Map.of("type", "select", "field", "choice", "title", "选项", "options", List.of(Map.of("label", "一", "value", "one"))),
                Map.of("type", "subForm", "field", "detail", "title", "分组"))));
        when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(model);
        model.setOrgId(100L);
        model.setManagerUserIds(List.of(8L));
        service.publish(11L, loginUserContext);
        verify(flowNodeValidator).validate(ArgumentMatchers.eq(model.getModelNode()),
                ArgumentMatchers.argThat(schema -> schema.getFields().stream()
                        .map(FlowVariableSchema.Field::getField).toList().equals(List.of("choice"))));
    }

    @Test
    void shouldPersistAndReturnDraftVariableSchema() throws Exception {
        var mapper = new ObjectMapper();
        var param = mapper.readValue("""
                {"id":11,"formMode":1,"varInfo":{"fields":[
                  {"field":"amount","title":"金额","type":1,"required":true}
                ]}}
                """, FlowModelParam.class);

        service.update(param, loginUserContext);

        ArgumentCaptor<FlowModel> captor = ArgumentCaptor.forClass(FlowModel.class);
        verify(flowModelDAO).updateById(captor.capture());
        var saved = captor.getValue();
        assertEquals(param.getVarInfo(), saved.getVarInfo());
        saved.setOrgId(100L);
        // 模拟 JSON 列序列化后重新读取，详情必须返回完整字段定义。
        saved.setVarInfo(mapper.readValue(mapper.writeValueAsString(saved.getVarInfo()), FlowVariableSchema.class));
        when(flowModelDAO.selectById(11L)).thenReturn(saved);
        var detail = service.detail(11L, loginUserContext);
        assertEquals(param.getVarInfo(), detail.getVarInfo());
        assertEquals("金额", detail.getVarInfo().getFields().getFirst().getTitle());
    }

    @Test
    void shouldPersistExplicitEmptyVariableSchemaWhenCreating() {
        var param = new FlowModelParam();
        param.setFormMode(1);
        param.setCode("business_create");
        param.setCategory("leave");
        param.setVarInfo(new FlowVariableSchema());

        service.create(param, loginUserContext);

        ArgumentCaptor<FlowModel> captor = ArgumentCaptor.forClass(FlowModel.class);
        verify(flowModelDAO).insert(captor.capture());
        assertEquals(List.of(), captor.getValue().getVarInfo().getFields());
    }

    @Test
    void shouldRejectChangedBusinessVariablesThenPublishConfirmedDraftSnapshot() throws Exception {
        var latest = new ObjectMapper().readValue("""
                {"fields":[{"field":"days","title":"请假天数","type":1,"required":true}]}
                """, FlowVariableSchema.class);
        var beans = new StaticListableBeanFactory();
        beans.addBean("variables", (FlowBusinessVariableProvider) (orgId, code) -> latest);
        ReflectionTestUtils.setField(flowVariables, "providers", beans.getBeanProvider(FlowBusinessVariableProvider.class));
        var model = flowModelDAO.selectFlowModelForUpdate(11L);
        model.setFormMode(1);
        model.setCode("leave-approval");
        model.setModelNode(new FlowNode());

        assertEquals("业务变量已调整，请刷新变量并适配工作流配置后重新发布",
                assertThrows(BizException.class, () -> service.publish(11L, loginUserContext)).getMessage());
        verifyNoInteractions(flowNodeValidator, flowDefinitionService);
        verify(flowModelDAO, Mockito.never()).updateById(ArgumentMatchers.any(FlowModel.class));

        var draft = new ObjectMapper().readValue(new ObjectMapper().writeValueAsString(latest), FlowVariableSchema.class);
        model.setVarInfo(draft);
        service.publish(11L, loginUserContext);
        var published = ArgumentCaptor.forClass(FlowDefinition.class);
        verify(flowDefinitionService).save(published.capture());
        assertSame(draft, published.getValue().getVarInfo());
        verify(flowNodeValidator).validate(model.getModelNode(), draft);
    }

    private FlowModelParam timeoutParam() {
        FlowModelParam param = new FlowModelParam();
        param.setId(11L);
        FlowExtraSetting setting = new FlowExtraSetting();
        setting.setTimeoutRule(new FlowTimeoutRule());
        param.setExtraSetting(setting);
        return param;
    }
}
