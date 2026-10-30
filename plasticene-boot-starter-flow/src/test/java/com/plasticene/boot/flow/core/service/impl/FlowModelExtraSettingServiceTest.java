package com.plasticene.boot.flow.core.service.impl;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.dto.FlowTimeoutRule;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.validator.FlowNodeValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @Mock
    private FlowModelDAO flowModelDAO;
    @Mock
    private CategoryService categoryService;
    @Mock
    private FlowNodeValidator flowNodeValidator;
    @Mock
    private FlowDefinitionService flowDefinitionService;
    @InjectMocks
    private FlowModelServiceImpl service;

    @Test
    void shouldRejectInvalidDefaultTimeoutWhenCreating() {
        FlowModelParam param = timeoutParam();
        param.getExtraSetting().getTimeoutRule().setAutoEnabled(true);

        assertThrows(BizException.class, () -> service.create(param));
        verifyNoInteractions(flowModelDAO);
    }

    @Test
    void shouldRejectInvalidDefaultTimeoutWhenUpdating() {
        FlowModelParam param = timeoutParam();
        param.getExtraSetting().getTimeoutRule().setReminderEnabled(true);

        assertThrows(BizException.class, () -> service.update(param));
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

        service.update(param);

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

        service.update(param);

        ArgumentCaptor<FlowModel> captor = ArgumentCaptor.forClass(FlowModel.class);
        verify(flowModelDAO).updateById(captor.capture());
        assertNull(captor.getValue().getExtraSetting());
    }

    @Test
    void shouldCopyExtraSettingToPublishedDefinition() {
        FlowModel model = new FlowModel();
        model.setId(11L);
        model.setModelNode(new FlowNode());
        FlowExtraSetting setting = new FlowExtraSetting();
        setting.setAllowSubmitterCancel(false);
        setting.setAllowApproverWithdraw(true);
        setting.setApproverDeduplication(FlowExtraSetting.ApproverDeduplication.ONCE_PER_PROCESS);
        model.setExtraSetting(setting);
        when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(model);
        when(flowDefinitionService.getMaxVersion(11L)).thenReturn(2);

        service.publish(11L);
        verify(flowNodeValidator).validate(model.getModelNode());

        ArgumentCaptor<FlowDefinition> captor = ArgumentCaptor.forClass(FlowDefinition.class);
        verify(flowDefinitionService).save(captor.capture());
        FlowExtraSetting snapshot = captor.getValue().getExtraSetting();
        assertEquals(false, snapshot.getAllowSubmitterCancel());
        assertEquals(true, snapshot.getAllowApproverWithdraw());
        assertEquals(FlowExtraSetting.ApproverDeduplication.ONCE_PER_PROCESS,
                snapshot.getApproverDeduplication());
        assertEquals(3, captor.getValue().getVersion());
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
