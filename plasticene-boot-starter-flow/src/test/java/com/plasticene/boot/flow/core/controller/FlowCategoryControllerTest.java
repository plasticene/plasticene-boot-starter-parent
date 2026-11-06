package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.flow.core.model.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证分类新增和修改接口真实启用字段校验。
 * @author ZFJ
 * @since 2026-11-06
 */
class FlowCategoryControllerTest {
    private FlowCategoryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(FlowCategoryService.class);
        var controller = new FlowCategoryController();
        ReflectionTestUtils.setField(controller, "flowCategoryService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abcd", "a b", "中文", "a.b", "123456789012345678901234567890123"})
    void shouldRejectInvalidCodeOnCreate(String code) throws Exception {
        mvc.perform(post("/flow/category").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"办公\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 32})
    void shouldAcceptBoundaryCodeAnd100CharacterDescription(int length) throws Exception {
        mvc.perform(post("/flow/category").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"办公\",\"code\":\"" + "_".repeat(length)
                        + "\",\"description\":\"" + "描".repeat(100) + "\"}"))
                .andExpect(status().isOk());
        verify(service).createCategory(any(FlowCategoryParam.class));
    }

    @Test
    void shouldReject101CharacterDescriptionForCreateAndUpdate() throws Exception {
        String body = "{\"id\":21,\"name\":\"办公\",\"code\":\"office\",\"description\":\"" + "描".repeat(101) + "\"}";
        mvc.perform(post("/flow/category").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/flow/category").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void shouldAcceptUnchangedLegacyShortCodeForUpdate() throws Exception {
        mvc.perform(put("/flow/category").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":21,\"name\":\"办公\",\"code\":\"old\"}"))
                .andExpect(status().isOk());
        verify(service).updateCategory(any(FlowCategoryParam.class));
    }

    @Test
    void shouldAllowUpdateWithoutCodeAndWithEmptyDescription() throws Exception {
        mvc.perform(put("/flow/category").contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":21,\"name\":\"办公\",\"description\":\"\"}"))
                .andExpect(status().isOk());
        verify(service).updateCategory(any(FlowCategoryParam.class));
    }
}
