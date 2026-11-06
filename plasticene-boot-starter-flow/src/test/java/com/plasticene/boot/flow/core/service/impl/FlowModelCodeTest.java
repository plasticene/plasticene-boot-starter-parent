package com.plasticene.boot.flow.core.service.impl;

import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.web.core.validator.Insert;
import com.plasticene.boot.web.core.validator.Update;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    @Mock
    private FlowModelDAO flowModelDAO;
    @InjectMocks
    private FlowModelServiceImpl service;

    @BeforeEach
    void setUp() {
        LoginUser user = new LoginUser();
        user.setOrgId(9L);
        LoginUserHolder.set(user);
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.remove();
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
                () -> service.create(param("tests"))).getMessage());
        verify(flowModelDAO, never()).insert(any(FlowModel.class));
    }

    @Test
    void shouldExcludeSelfOnUpdate() {
        var param = param("tests");
        param.setId(11L);
        service.update(param);
        verify(flowModelDAO).countByCode(9L, "tests", 11L);
        verify(flowModelDAO).updateById(any(FlowModel.class));
    }

    @Test
    void shouldRejectDuplicateOnUpdate() {
        var param = param("tests");
        param.setId(11L);
        when(flowModelDAO.countByCode(9L, "tests", 11L)).thenReturn(1L);
        assertThrows(BizException.class, () -> service.update(param));
        verify(flowModelDAO, never()).updateById(any(FlowModel.class));
    }

    @Test
    void shouldTranslateConcurrentDuplicateInsert() {
        when(flowModelDAO.insert(any(FlowModel.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertEquals("流程标识已存在", assertThrows(BizException.class,
                () -> service.create(param("tests"))).getMessage());
    }

    @Test
    void shouldAllowPartialUpdateWithoutCode() {
        var param = param(null);
        param.setId(11L);
        service.update(param);
        verify(flowModelDAO, never()).countByCode(any(), any(), any());
        verify(flowModelDAO).updateById(any(FlowModel.class));
    }

    @Test
    void shouldKeepLegacyShortCodeOnlyWhenOriginalAndCompanyMatch() {
        var existing = new FlowModel();
        existing.setId(11L);
        existing.setOrgId(9L);
        existing.setCode("old");
        when(flowModelDAO.selectById(11L)).thenReturn(existing);
        var param = param("old");
        param.setId(11L);
        assertDoesNotThrow(() -> service.update(param));
        verify(flowModelDAO).updateById(any(FlowModel.class));
        param.setCode("new");
        assertThrows(BizException.class, () -> service.update(param));
        param.setCode("old");
        existing.setOrgId(10L);
        assertThrows(BizException.class, () -> service.update(param));
        assertThrows(BizException.class, () -> service.create(param("abcd")));
        verify(flowModelDAO, times(1)).updateById(any(FlowModel.class));
    }

    private FlowModelParam param(String code) {
        var param = new FlowModelParam();
        param.setCode(code);
        return param;
    }
}
