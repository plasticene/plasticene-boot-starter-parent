package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.dao.FlowFormDAO;
import com.plasticene.boot.flow.core.entity.FlowForm;
import com.plasticene.boot.flow.core.model.param.FlowFormParam;
import com.plasticene.boot.flow.core.model.query.FlowFormQuery;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证流程表单保存、校验及分类查询。
 * @author ZFJ
 * @since 2026-11-05
 */
@ExtendWith(MockitoExtension.class)
class FlowFormServiceImplTest {
    private LoginUser loginUserContext;
    @Mock
    private FlowFormDAO flowFormDAO;
    @Mock
    private FlowCategoryService flowCategoryService;
    @InjectMocks
    private FlowFormServiceImpl service;

    @BeforeEach
    void setUpLoginUser() {
        LoginUser user = new LoginUser();
        user.setId(8L);
        user.setOrgId(9L);
        loginUserContext = user;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"type\":\"input\",\"field\":\"name\",\"title\":\" \"}",
            "{\"type\":\"select\",\"field\":\"choice\",\"title\":\"选择\",\"options\":[]}",
            "{\"type\":\"radio\",\"field\":\"choice\",\"title\":\"选择\",\"options\":[{\"value\":1}]}",
            "{\"type\":\"checkbox\",\"field\":\"choice\",\"title\":\"选择\",\"options\":[{\"label\":\"选项\"}]}",
            "{broken"
    })
    void shouldRejectInvalidSerializedFieldsBeforePersistence(String rule) {
        var param = param(List.of(rule));
        assertThrows(BizException.class, () -> service.create(param, loginUserContext));
        assertThrows(BizException.class, () -> service.update(param));
        verifyNoInteractions(flowFormDAO);
    }

    @Test
    void shouldRejectDuplicateFieldsAcrossSerializedRulesAndNestedLayouts() {
        String rule = "{\"type\":\"input\",\"field\":\"name\",\"title\":\"姓名\"}";
        var param = param(List.of(rule, "{\"type\":\"row\",\"children\":[" + rule + "]}"));
        assertThrows(BizException.class, () -> service.create(param, loginUserContext));
        assertThrows(BizException.class, () -> service.update(param));
        verifyNoInteractions(flowFormDAO);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSaveValidSerializedFieldsWithoutChangingStoredFormat(boolean create) {
        var param = param(List.of("{\"type\":\"select\",\"field\":\"choice\",\"title\":\"选择\",\"options\":[{\"label\":\"零\",\"value\":0},{\"label\":\"否\",\"value\":false}]}"));
        param.setIcon("flow:form");
        var captured = ArgumentCaptor.forClass(FlowForm.class);
        if (create) {
            when(flowFormDAO.insert(any(FlowForm.class))).thenAnswer(call -> {
                call.<FlowForm>getArgument(0).setId(21L);
                return 1;
            });
            assertEquals(21L, service.create(param, loginUserContext));
            verify(flowFormDAO).insert(captured.capture());
            assertEquals(9L, captured.getValue().getOrgId());
        } else {
            service.update(param);
            verify(flowFormDAO).updateById(captured.capture());
            assertEquals(11L, captured.getValue().getId());
        }
        assertEquals("flow:form", captured.getValue().getIcon());
        assertEquals(param.getFormConfig(), captured.getValue().getFormConfig());
        assertEquals("测试表单", captured.getValue().getName());
        assertEquals("office", captured.getValue().getCategory());
    }

    @Test
    void shouldAllowMetadataOnlyUpdateAndEmptyRules() {
        var param = param(null);
        service.update(param);
        var captured = ArgumentCaptor.forClass(FlowForm.class);
        verify(flowFormDAO).updateById(captured.capture());
        assertNull(captured.getValue().getFormConfig());
        param.setFormConfig(Map.of("rule", List.of(), "option", Map.of()));
        assertDoesNotThrow(() -> service.update(param));
    }

    @Test
    void shouldUpdateStatusWithoutOverwritingConfigOrName() {
        var param = new FlowFormParam();
        param.setId(11L);
        param.setStatus(0);
        service.update(param);
        var captured = ArgumentCaptor.forClass(FlowForm.class);
        verify(flowFormDAO).updateById(captured.capture());
        assertEquals(0, captured.getValue().getStatus());
        assertNull(captured.getValue().getFormConfig());
        assertNull(captured.getValue().getName());
        assertNull(captured.getValue().getCategory());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldFilterByCategoryAndReturnCategoryName(boolean filtered) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "form"), FlowForm.class);
        var query = new FlowFormQuery();
        if (filtered) {
            query.setCategory("office");
        }
        var form = new FlowForm();
        form.setCategory("office");
        when(flowCategoryService.getCategoryMap(loginUserContext)).thenReturn(Map.of("office", "办公"));
        when(flowFormDAO.selectPage(eq(query), any(PtcLambdaQueryWrapper.class)))
                .thenAnswer(call -> {
                    PtcLambdaQueryWrapper<FlowForm> wrapper = call.getArgument(1);
                    String sql = wrapper.getSqlSegment();
                    assertTrue(sql.contains("org_id"));
                    assertEquals(filtered, sql.contains("category"));
                    assertTrue(wrapper.getParamNameValuePairs().containsValue(9L));
                    assertEquals(filtered, wrapper.getParamNameValuePairs().containsValue("office"));
                    return new PageResult<>(List.of(form), 1L, 1L);
                });
        var result = service.page(query, loginUserContext);
        assertEquals("office", result.getList().getFirst().getCategory());
        assertEquals("办公", result.getList().getFirst().getCategoryName());
    }

    private FlowFormParam param(List<String> fields) {
        var param = new FlowFormParam();
        param.setId(11L);
        param.setName("测试表单");
        param.setCategory("office");
        param.setFormConfig(fields == null ? null : Map.of("option", Map.of("form", Map.of("layout", "horizontal")), "rule", fields));
        return param;
    }
}
