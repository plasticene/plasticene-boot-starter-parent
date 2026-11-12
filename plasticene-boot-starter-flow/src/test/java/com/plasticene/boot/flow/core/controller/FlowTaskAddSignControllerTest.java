package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.enums.FlowAddSignEnum;
import com.plasticene.boot.flow.core.model.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证加签接口的类型、人员列表及原因参数契约。
 * @author ZFJ
 * @since 2026-11-03
 */
class FlowTaskAddSignControllerTest {
    private final LoginUser loginUser = new LoginUser();
    private final FlowTaskService service = mock(FlowTaskService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        loginUser.setId(17L);
        loginUser.setOrgId(27L);
        LoginUserHolder.set(loginUser);
        FlowTaskController controller = new FlowTaskController();
        ReflectionTestUtils.setField(controller, "flowTaskService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldPreserveSelectedOrderAndSignType() throws Exception {
        mvc.perform(post("/flow/task/add-sign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":1,\"type\":\"BEFORE\",\"assigneeIds\":[11,10],\"approveMode\":2,\"comment\":\"补充审核\"}"))
                .andExpect(status().isOk());
        var param = ArgumentCaptor.forClass(FlowTaskAddSignParam.class);
        verify(service).addSign(param.capture(), ArgumentMatchers.same(loginUser));
        assertEquals(FlowAddSignEnum.Type.BEFORE, param.getValue().getType());
        assertEquals(List.of(11L, 10L), param.getValue().getAssigneeIds());
        assertEquals(2, param.getValue().getApproveMode());
    }

    @Test
    void shouldRejectMissingOrInvalidInputs() throws Exception {
        for (String payload : List.of(
                "{\"taskId\":0,\"type\":\"BEFORE\",\"assigneeIds\":[10],\"comment\":\"原因\"}",
                "{\"taskId\":1,\"assigneeIds\":[10],\"comment\":\"原因\"}",
                "{\"taskId\":1,\"type\":\"INVALID\",\"assigneeIds\":[10],\"comment\":\"原因\"}",
                "{\"taskId\":1,\"type\":\"BEFORE\",\"assigneeIds\":[],\"comment\":\"原因\"}",
                "{\"taskId\":1,\"type\":\"BEFORE\",\"assigneeIds\":[null],\"comment\":\"原因\"}",
                "{\"taskId\":1,\"type\":\"BEFORE\",\"assigneeIds\":[-1],\"comment\":\"原因\"}",
                "{\"taskId\":1,\"type\":\"BEFORE\",\"assigneeIds\":[10],\"comment\":\"   \"}",
                "{\"taskId\":1,\"type\":\"BEFORE\",\"assigneeIds\":[10],\"comment\":\"" + "x".repeat(501) + "\"}")) {
            mvc.perform(post("/flow/task/add-sign").contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @AfterEach
    void clearLoginUser() {
        LoginUserHolder.remove();
    }
}
