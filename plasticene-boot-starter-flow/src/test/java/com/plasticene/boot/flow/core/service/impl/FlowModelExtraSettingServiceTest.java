package com.plasticene.boot.flow.core.service.impl;

import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
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
import static org.mockito.Mockito.verify;
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

        ArgumentCaptor<FlowDefinition> captor = ArgumentCaptor.forClass(FlowDefinition.class);
        verify(flowDefinitionService).save(captor.capture());
        FlowExtraSetting snapshot = captor.getValue().getExtraSetting();
        assertEquals(false, snapshot.getAllowSubmitterCancel());
        assertEquals(true, snapshot.getAllowApproverWithdraw());
        assertEquals(FlowExtraSetting.ApproverDeduplication.ONCE_PER_PROCESS,
                snapshot.getApproverDeduplication());
        assertEquals(3, captor.getValue().getVersion());
    }
}
