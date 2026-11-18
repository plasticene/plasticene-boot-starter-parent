package com.plasticene.boot.flow.core.impl.service.model;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowModelParam;
import com.plasticene.boot.flow.core.impl.application.FlowVariables;
import com.plasticene.boot.flow.core.impl.persistence.dao.*;
import com.plasticene.boot.flow.core.impl.persistence.entity.*;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.validation.FlowNodeValidator;
import com.plasticene.boot.flow.core.model.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证设计态表单类型双向切换仅在发布后生成新快照。
 * @author ZFJ
 * @since 2026-11-16
 */
@ExtendWith(MockitoExtension.class)
class FlowModelModeTest {
    @Mock FlowModelDAO flowModelDAO;
    @Mock FlowCategoryDAO flowCategoryDAO;
    @Mock FlowDefinitionService flowDefinitionService;
    @Mock FlowNodeValidator flowNodeValidator;
    @Mock FlowVariables flowVariables;
    @InjectMocks FlowModelServiceImpl service;
    private final FlowModel model = new FlowModel();
    private final LoginUser actor = new LoginUser();
    @BeforeEach
    void setup() {
        actor.setId(8L); actor.setOrgId(9L);
        model.setId(11L); model.setOrgId(9L); model.setManagerUserIds(List.of(8L));
        model.setCategory("office"); model.setCode("approval"); model.setFormMode(0);
        model.setModelNode(new FlowNode()); model.setActiveDefinitionId(31L);
        model.setFormConfig(Map.of("rule", List.of()));
        when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(model);
    }
    @Test
    void savingBusinessDraftDoesNotReplaceCurrentPublishedDefinition() {
        var param = new FlowModelParam(); param.setId(11L); param.setFormMode(1);
        service.update(param, actor);
        var patch = ArgumentCaptor.forClass(FlowModel.class);
        verify(flowModelDAO).updateById(patch.capture());
        assertEquals(1, patch.getValue().getFormMode());
        assertNull(patch.getValue().getActiveDefinitionId());
        assertEquals(31L, model.getActiveDefinitionId());
        verifyNoInteractions(flowDefinitionService);
    }
    @Test
    void republishingBothModesKeepsEarlierDefinitionsAndFormConfigImmutable() {
        when(flowCategoryDAO.selectByCodeForUpdate(9L, "office")).thenReturn(new FlowCategory());
        when(flowVariables.forModel(model)).thenAnswer(call -> new FlowVariableSchema());
        when(flowDefinitionService.getMaxVersion(11L)).thenReturn(0, 1, 2);
        service.publish(11L, actor);
        model.setFormMode(1);
        service.publish(11L, actor);
        model.setFormMode(0);
        service.publish(11L, actor);
        var published = ArgumentCaptor.forClass(FlowDefinition.class);
        verify(flowDefinitionService, times(3)).save(published.capture());
        var versions = published.getAllValues();
        assertEquals(List.of(0, 1, 0), versions.stream().map(FlowDefinition::getFormMode).toList());
        assertEquals(List.of(1, 2, 3), versions.stream().map(FlowDefinition::getVersion).toList());
        assertNotNull(versions.get(0).getFormConfig()); assertNull(versions.get(1).getFormConfig());
        assertNotNull(versions.get(2).getFormConfig());
        assertNotSame(versions.get(0).getVarInfo(), versions.get(1).getVarInfo());
        assertNotNull(model.getFormConfig());
    }
}
