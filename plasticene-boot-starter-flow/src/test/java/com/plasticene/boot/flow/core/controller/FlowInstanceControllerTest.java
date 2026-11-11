package com.plasticene.boot.flow.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
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
 * 验证流程实例接口按字符串接收和返回业务标识。
 * @author ZFJ
 * @since 2026-11-11
 */
@ExtendWith(MockitoExtension.class)
class FlowInstanceControllerTest {
    @Mock
    private FlowRuntimeService service;
    @InjectMocks
    private FlowInstanceController controller;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"BIZ-000041", "000041", "9223372036854775808"})
    void shouldAcceptStringOrOmittedBusinessId(String businessId) throws Exception {
        var mapper = new ObjectMapper();
        var payload = mapper.createObjectNode().put("definitionId", 31L);
        payload.putObject("varMap");
        if (businessId != null) {
            payload.put("businessId", businessId);
        }
        mvc.perform(post("/flow/instance/start").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
        verify(service).startFlowInstanceById(31L, businessId, Map.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BIZ-000041", "000041", "9223372036854775808"})
    void shouldReturnBusinessIdAsUnmodifiedJsonString(String businessId) throws Exception {
        FlowInstanceVO instance = new FlowInstanceVO();
        instance.setBusinessId(businessId);
        FlowInstanceDetailVO detail = new FlowInstanceDetailVO();
        detail.setInstance(instance);
        when(service.getInstanceDetail(21L, null)).thenReturn(detail);
        mvc.perform(get("/flow/instance/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instance.businessId").isString())
                .andExpect(jsonPath("$.data.instance.businessId").value(businessId));
    }
}
