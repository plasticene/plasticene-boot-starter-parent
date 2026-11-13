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
import com.plasticene.boot.flow.core.impl.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryService;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.validation.node.FlowNodeValidator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        verify(flowNodeValidator).validate(model.getModelNode(), null);

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
    void shouldOnlyPassEligibleFieldsWhenPublishingWithoutRevalidatingForm() {
        var model = new FlowModel();
        model.setCategory("leave");
        model.setModelNode(new FlowNode());
        model.setFormConfig(Map.of("rule", List.of(
                Map.of("type", "select", "field", "choice", "title", ""),
                Map.of("type", "subForm", "field", "detail", "title", "分组"))));
        when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(model);
        model.setOrgId(100L);
        model.setManagerUserIds(List.of(8L));
        service.publish(11L, loginUserContext);
        verify(flowNodeValidator).validate(model.getModelNode(), Set.of("choice"));
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
