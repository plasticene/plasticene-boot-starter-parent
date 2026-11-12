package com.plasticene.boot.flow.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 Web 发起不要求业务标识，实例详情继续返回业务标识。
 * @author ZFJ
 * @since 2026-11-11
 */
@ExtendWith(MockitoExtension.class)
class FlowInstanceControllerTest {
    private final LoginUser loginUser = new LoginUser();
    @Mock
    private FlowRuntimeService service;
    @InjectMocks
    private FlowInstanceController controller;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        loginUser.setId(17L);
        loginUser.setOrgId(27L);
        LoginUserHolder.set(loginUser);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldStartWebInstanceWithoutBusinessId() throws Exception {
        var mapper = new ObjectMapper();
        var payload = mapper.createObjectNode().put("definitionId", 31L);
        payload.putObject("varMap");
        when(service.startInstance(31L, Map.of(), loginUser)).thenReturn(41L);
        mvc.perform(post("/flow/instance/start").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(41L));
        verify(service).startInstance(31L, Map.of(), loginUser);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BIZ-000041", "000041", "9223372036854775808"})
    void shouldReturnBusinessIdAsUnmodifiedJsonString(String businessId) throws Exception {
        FlowInstanceVO instance = new FlowInstanceVO();
        instance.setBusinessId(businessId);
        FlowInstanceDetailVO detail = new FlowInstanceDetailVO();
        detail.setInstance(instance);
        when(service.getInstanceDetail(21L, null, loginUser)).thenReturn(detail);
        mvc.perform(get("/flow/instance/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instance.businessId").isString())
                .andExpect(jsonPath("$.data.instance.businessId").value(businessId));
    }

    @AfterEach
    void clearLoginUser() {
        LoginUserHolder.remove();
    }
}
