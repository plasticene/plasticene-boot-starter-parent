package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.model.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证审批转交接口的参数契约和必填校验。
 * @author ZFJ
 * @since 2026-11-02
 */
@ExtendWith(MockitoExtension.class)
class FlowTaskTransferControllerTest {
    private final LoginUser loginUser = new LoginUser();
    @Mock
    private FlowTaskService service;
    @InjectMocks
    private FlowTaskController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        loginUser.setId(17L);
        loginUser.setOrgId(27L);
        LoginUserHolder.set(loginUser);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldAcceptTransferPayload() throws Exception {
        mvc.perform(post("/flow/task/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":11,\"assignee\":10,\"comment\":\"出差交接\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<FlowTaskTransferParam> param = ArgumentCaptor.forClass(FlowTaskTransferParam.class);
        verify(service).transferTask(param.capture(), ArgumentMatchers.same(loginUser));
        assertEquals(11L, param.getValue().getTaskId());
        assertEquals(10L, param.getValue().getAssignee());
        assertEquals("出差交接", param.getValue().getComment());
    }

    @Test
    void shouldRejectMissingReasonOrInvalidRecipient() throws Exception {
        mvc.perform(post("/flow/task/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":11,\"assignee\":10,\"comment\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/flow/task/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":11,\"assignee\":0,\"comment\":\"出差交接\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectOverlongTransferReason() throws Exception {
        String payload = "{\"taskId\":11,\"assignee\":10,\"comment\":\"" + "x".repeat(501) + "\"}";
        mvc.perform(post("/flow/task/transfer").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @AfterEach
    void clearLoginUser() {
        LoginUserHolder.remove();
    }
}
