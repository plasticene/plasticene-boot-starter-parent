package com.plasticene.boot.flow.core.impl.service.category;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowCategoryDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowCategory;
import com.plasticene.boot.flow.core.impl.model.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.impl.model.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.impl.validation.group.Insert;
import com.plasticene.boot.flow.core.impl.validation.group.Update;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.validation.Validation;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证分类标识、描述、公司隔离及引用保护规则。
 * @author ZFJ
 * @since 2026-11-06
 */
@ExtendWith(MockitoExtension.class)
class FlowCategoryServiceImplTest {
    private LoginUser loginUserContext;
    @Mock
    private FlowCategoryDAO flowCategoryDAO;
    @InjectMocks
    private FlowCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        LoginUser user = new LoginUser();
        user.setOrgId(9L);
        loginUserContext = user;
        lenient().when(flowCategoryDAO.findModelReference(anyLong(), anyString())).thenReturn(null);
        lenient().when(flowCategoryDAO.findFormReference(anyLong(), anyString())).thenReturn(null);
        lenient().when(flowCategoryDAO.findDefinitionReference(anyLong(), anyString())).thenReturn(null);
        lenient().when(flowCategoryDAO.findInstanceReference(anyLong(), anyString())).thenReturn(null);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "category"), FlowCategory.class);
    }

    @Test
    void shouldValidateCodeAndDescriptionForBothGroups() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Class<?> group : new Class<?>[]{Insert.class, Update.class}) {
                for (String code : new String[]{"abcde", "12345", "_____", "-abcd", "A_0-z", "a".repeat(32)}) {
                    assertTrue(validator.validateProperty(param(code), "code", group).isEmpty(), code);
                }
                for (String code : new String[]{"", " ", "a b", "中文", "a.b", "a/1", "a".repeat(33)}) {
                    assertFalse(validator.validateProperty(param(code), "code", group).isEmpty(), code);
                }
                for (String description : new String[]{"", "描".repeat(100)}) {
                    var param = param("office");
                    param.setDescription(description);
                    assertTrue(validator.validateProperty(param, "description", group).isEmpty());
                }
                var param = param("office");
                assertTrue(validator.validateProperty(param, "description", group).isEmpty());
                param.setDescription("描".repeat(101));
                assertFalse(validator.validateProperty(param, "description", group).isEmpty());
            }
            assertFalse(validator.validateProperty(param("abcd"), "code", Insert.class).isEmpty());
            assertTrue(validator.validateProperty(param("abcd"), "code", Update.class).isEmpty());
            assertFalse(validator.validateProperty(param(null), "code", Insert.class).isEmpty());
            assertTrue(validator.validateProperty(param(null), "code", Update.class).isEmpty());
        }
    }

    @Test
    void shouldRejectDuplicateAndTranslateConcurrentConflict() {
        when(flowCategoryDAO.countByCode(9L, "Office")).thenReturn(1L);
        assertEquals("分类标识已存在", assertThrows(BizException.class,
                () -> service.createCategory(param("Office"), loginUserContext)).getMessage());
        verify(flowCategoryDAO, never()).insert(any(FlowCategory.class));
        when(flowCategoryDAO.countByCode(9L, "Office")).thenReturn(0L);
        when(flowCategoryDAO.insert(any(FlowCategory.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertEquals("分类标识已存在", assertThrows(BizException.class,
                () -> service.createCategory(param("Office"), loginUserContext)).getMessage());
    }

    @Test
    void shouldCreateInCurrentCompanyWithDescription() {
        var param = param("Office");
        param.setDescription("描述");
        param.setId(99L);
        when(flowCategoryDAO.insert(any(FlowCategory.class))).thenAnswer(call -> {
            FlowCategory value = call.getArgument(0);
            assertNull(value.getId());
            assertEquals(9L, value.getOrgId());
            assertEquals("Office", value.getCode());
            assertEquals("描述", value.getDescription());
            assertEquals(0, value.getIsSys());
            value.setId(21L);
            return 1;
        });
        assertEquals(21L, service.createCategory(param, loginUserContext));
        verify(flowCategoryDAO).countByCode(9L, "Office");
    }

    @ParameterizedTest
    @ValueSource(strings = {"office", "other"})
    void shouldRejectAnyCodeChangeIncludingCase(String code) {
        when(flowCategoryDAO.selectForUpdate(9L, 21L)).thenReturn(category(21L));
        var param = param(code);
        param.setId(21L);
        assertEquals("分类标识创建后不可修改", assertThrows(BizException.class,
                () -> service.updateCategory(param, loginUserContext)).getMessage());
        verify(flowCategoryDAO, never()).update(any(FlowCategory.class), any(Wrapper.class));
    }

    @Test
    void shouldUpdateMetadataAndExplicitlyClearDescription() {
        when(flowCategoryDAO.selectForUpdate(9L, 21L)).thenReturn(category(21L));
        var param = param(null);
        param.setId(21L);
        param.setIcon("flow:form");
        when(flowCategoryDAO.update(any(FlowCategory.class), any(Wrapper.class))).thenAnswer(call -> {
            var wrapper = (LambdaUpdateWrapper<FlowCategory>) call.getArgument(1);
            assertTrue(wrapper.getSqlSet().contains("description="));
            assertTrue(wrapper.getParamNameValuePairs().containsValue(null));
            wrapper.getSqlSegment();
            assertTrue(wrapper.getParamNameValuePairs().containsValue(9L));
            assertFalse(wrapper.getSqlSet().contains("code="));
            return 1;
        });
        service.updateCategory(param, loginUserContext);
        param.setCode("Office");
        service.updateCategory(param, loginUserContext);
        verify(flowCategoryDAO, times(2)).update(any(FlowCategory.class), any(Wrapper.class));
    }

    @Test
    void shouldRejectUnknownOrOtherCompanyCategoryForUpdateDeleteAndSort() {
        var param = param("Office");
        param.setId(21L);
        assertThrows(BizException.class, () -> service.updateCategory(param, loginUserContext));
        assertThrows(BizException.class, () -> service.delete(List.of(21L), loginUserContext));
        assertThrows(BizException.class, () -> service.sortCategory(List.of(21L), loginUserContext));
        verify(flowCategoryDAO, never()).deleteByIds(anyCollection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"model", "form", "definition", "instance"})
    void shouldRejectEachKindOfReference(String kind) {
        when(flowCategoryDAO.selectForUpdate(9L, 21L)).thenReturn(category(21L));
        String expected;
        switch (kind) {
            case "model" -> { when(flowCategoryDAO.findModelReference(9L, "Office")).thenReturn(1L); expected = "流程模型"; }
            case "form" -> { when(flowCategoryDAO.findFormReference(9L, "Office")).thenReturn(1L); expected = "流程表单"; }
            case "definition" -> { when(flowCategoryDAO.findDefinitionReference(9L, "Office")).thenReturn(1L); expected = "流程发布版本"; }
            default -> { when(flowCategoryDAO.findInstanceReference(9L, "Office")).thenReturn(1L); expected = "流程实例"; }
        }
        assertTrue(assertThrows(BizException.class, () -> service.delete(List.of(21L), loginUserContext)).getMessage().contains(expected));
        verify(flowCategoryDAO, never()).deleteByIds(anyCollection());
    }

    @Test
    void shouldRejectSystemCategoryAndKeepBatchAtomic() {
        FlowCategory system = category(22L);
        system.setIsSys(1);
        when(flowCategoryDAO.selectForUpdate(9L, 21L)).thenReturn(category(21L));
        when(flowCategoryDAO.selectForUpdate(9L, 22L)).thenReturn(system);
        assertThrows(BizException.class, () -> service.delete(List.of(22L, 21L), loginUserContext));
        verify(flowCategoryDAO, never()).deleteByIds(anyCollection());
    }

    @Test
    void shouldLockBatchInStableOrderAndDeleteOnlyAfterAllChecks() {
        when(flowCategoryDAO.selectForUpdate(9L, 21L)).thenReturn(category(21L));
        when(flowCategoryDAO.selectForUpdate(9L, 22L)).thenReturn(category(22L));
        service.delete(List.of(22L, 21L, 21L), loginUserContext);
        var order = inOrder(flowCategoryDAO);
        order.verify(flowCategoryDAO).selectForUpdate(9L, 21L);
        order.verify(flowCategoryDAO).selectForUpdate(9L, 22L);
        order.verify(flowCategoryDAO, times(2)).findModelReference(9L, "Office");
        order.verify(flowCategoryDAO).deleteByIds(List.of(21L, 22L));
    }

    @Test
    void shouldFilterByNameAndCodeWithinCurrentCompany() {
        var query = new FlowCategoryQuery();
        query.setName("办公");
        query.setCode("office");
        when(flowCategoryDAO.selectPage(eq(query), any(PtcLambdaQueryWrapper.class))).thenAnswer(call -> {
            PtcLambdaQueryWrapper<FlowCategory> wrapper = call.getArgument(1);
            String sql = wrapper.getSqlSegment();
            assertTrue(sql.contains("org_id"));
            assertTrue(sql.contains("code ="));
            assertTrue(sql.contains("name LIKE"));
            wrapper.getSqlSegment();
            assertTrue(wrapper.getParamNameValuePairs().containsValue(9L));
            return new PageResult<>(List.of(category(21L)), 1L, 1L);
        });
        assertEquals("Office", service.page(query, loginUserContext).getList().getFirst().getCode());
    }

    @Test
    void shouldKeepLegacyShortCodeWhileEditingMetadata() {
        var existing = category(21L);
        existing.setCode("old");
        when(flowCategoryDAO.selectForUpdate(9L, 21L)).thenReturn(existing);
        var param = param("old");
        param.setId(21L);
        service.updateCategory(param, loginUserContext);
        verify(flowCategoryDAO).update(any(FlowCategory.class), any(Wrapper.class));
        param.setCode("new");
        assertThrows(BizException.class, () -> service.updateCategory(param, loginUserContext));
    }

    private FlowCategory category(Long id) {
        FlowCategory category = new FlowCategory();
        category.setId(id);
        category.setOrgId(9L);
        category.setName("办公");
        category.setCode("Office");
        category.setIsSys(0);
        return category;
    }

    private FlowCategoryParam param(String code) {
        FlowCategoryParam param = new FlowCategoryParam();
        param.setName("办公");
        param.setCode(code);
        return param;
    }
}
