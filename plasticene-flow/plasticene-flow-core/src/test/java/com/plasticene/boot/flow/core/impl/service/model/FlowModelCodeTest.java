package com.plasticene.boot.flow.core.impl.service.model;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowCategoryDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowCategory;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.flow.core.impl.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.impl.service.definition.FlowDefinitionService;
import com.plasticene.boot.flow.core.impl.validation.group.Insert;
import com.plasticene.boot.flow.core.impl.validation.group.Update;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证流程标识格式、长度和组织内唯一性。
 * @author ZFJ
 * @since 2026-11-05
 */
@ExtendWith(MockitoExtension.class)
class FlowModelCodeTest {
    private LoginUser loginUserContext;
    @Mock
    private FlowModelDAO flowModelDAO;
    @InjectMocks
    private FlowModelServiceImpl service;

    @Mock
    private FlowCategoryDAO flowCategoryDAO;

    @Mock
    private FlowDefinitionService flowDefinitionService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(flowCategoryDAO.selectByCodeForUpdate(
                ArgumentMatchers.anyLong(), ArgumentMatchers.anyString()))
                .thenReturn(new FlowCategory());

        LoginUser user = new LoginUser();
        user.setOrgId(9L);
        user.setId(8L);
        loginUserContext = user;
        FlowModel existing = new FlowModel();
        existing.setCategory("leave");
        existing.setId(11L);
        existing.setOrgId(9L);
        existing.setManagerUserIds(List.of(8L));
        lenient().when(flowModelDAO.selectById(11L)).thenReturn(existing);
        lenient().when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(existing);
    }

    @Test
    void shouldValidateCodeForCreateAndUpdate() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Class<?> group : new Class<?>[]{Insert.class, Update.class}) {
                for (String code : new String[]{"abcde", "12345", "0_A-z", "a".repeat(32)}) {
                    var param = param(code);
                    assertTrue(validator.validateProperty(param, "code", group).isEmpty(), code);
                }
                for (String code : new String[]{"", " ", "_abcd", "-abcd", "a.b", "a$b", "中文", "a b", "a".repeat(33)}) {
                    assertFalse(validator.validateProperty(param(code), "code", group).isEmpty(), code);
                }
            }
            assertFalse(validator.validateProperty(param("abcd"), "code", Insert.class).isEmpty());
            assertTrue(validator.validateProperty(param("abcd"), "code", Update.class).isEmpty());
            assertFalse(validator.validateProperty(param(null), "code", Insert.class).isEmpty());
            assertTrue(validator.validateProperty(param(null), "code", Update.class).isEmpty());
        }
    }

    @Test
    void shouldRejectDuplicateOnCreate() {
        when(flowModelDAO.countByCode(9L, "tests", null)).thenReturn(1L);
        assertEquals("流程标识已存在", assertThrows(BizException.class,
                () -> service.create(param("tests"), loginUserContext)).getMessage());
        verify(flowModelDAO, never()).insert(any(FlowModel.class));
    }

    @Test
    void shouldExcludeSelfOnUpdate() {
        var param = param("tests");
        param.setId(11L);
        service.update(param, loginUserContext);
        verify(flowModelDAO).countByCode(9L, "tests", 11L);
        verify(flowModelDAO).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectDuplicateOnUpdate() {
        var param = param("tests");
        param.setId(11L);
        when(flowModelDAO.countByCode(9L, "tests", 11L)).thenReturn(1L);
        assertThrows(BizException.class, () -> service.update(param, loginUserContext));
        verify(flowModelDAO, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldTranslateConcurrentDuplicateInsert() {
        when(flowModelDAO.insert(any(FlowModel.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertEquals("流程标识已存在", assertThrows(BizException.class,
                () -> service.create(param("tests"), loginUserContext)).getMessage());
    }

    @Test
    void shouldAllowPartialUpdateWithoutCode() {
        var param = param(null);
        param.setId(11L);
        service.update(param, loginUserContext);
        verify(flowModelDAO, never()).countByCode(any(), any(), any());
        verify(flowModelDAO).updateById(any(FlowModel.class));
    }

    @Test
    void shouldKeepLegacyShortCodeOnlyWhenOriginalAndCompanyMatch() {
        var existing = new FlowModel();
        existing.setId(11L);
        existing.setOrgId(9L);
        existing.setCode("old");
        existing.setManagerUserIds(List.of(8L));
        when(flowModelDAO.selectById(11L)).thenReturn(existing);
        lenient().when(flowModelDAO.selectFlowModelForUpdate(11L)).thenReturn(existing);
        var param = param("old");
        param.setId(11L);
        assertDoesNotThrow(() -> service.update(param, loginUserContext));
        verify(flowModelDAO).updateById(any(FlowModel.class));
        param.setCode("new");
        assertThrows(BizException.class, () -> service.update(param, loginUserContext));
        param.setCode("old");
        existing.setOrgId(10L);
        assertThrows(BizException.class, () -> service.update(param, loginUserContext));
        assertThrows(BizException.class, () -> service.create(param("abcd"), loginUserContext));
        verify(flowModelDAO, times(1)).updateById(any(FlowModel.class));
    }

    @Test
    void shouldAlwaysCreateDraftRegardlessOfRequestedStatus() {
        when(flowModelDAO.insert(any(FlowModel.class))).thenAnswer(invocation -> {
            FlowModel model = invocation.getArgument(0);
            assertEquals(0, model.getStatus());
            assertNull(model.getActiveDefinitionId());
            assertNull(model.getActiveVersion());
            assertNull(model.getPublishTime());
            model.setId(88L);
            return 1;
        });
        for (Integer status : new Integer[]{null, 0, 1, -1}) {
            var param = param("tests_copy");
            param.setStatus(status);
            assertEquals(88L, service.create(param, loginUserContext));
        }
    }

    private FlowModelParam param(String code) {
        var param = new FlowModelParam();
        param.setCategory("leave");
        param.setCode(code);
        return param;
    }
}
