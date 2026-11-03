package com.plasticene.boot.flow.core.controller;

import com.plasticene.boot.flow.core.model.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.model.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 验证审批退回接口的参数校验、响应结构及服务调用。
 * @author ZFJ
 * @since 2026-11-02
 */
class FlowTaskReturnControllerTest {
    private final FlowTaskService service = mock(FlowTaskService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        FlowTaskController controller = new FlowTaskController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "flowTaskService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldExposeOnlyNodeKeysAndNamesAsReturnTargets() throws Exception {
        when(service.listReturnTargets(11L)).thenReturn(List.of(new FlowTaskReturnTargetVO("a", "部门审批")));
        mvc.perform(get("/flow/task/11/return-targets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nodeKey").value("a"))
                .andExpect(jsonPath("$.data[0].nodeName").value("部门审批"));
    }

    @Test
    void shouldPassReturnPayloadToTaskService() throws Exception {
        mvc.perform(post("/flow/task/return").contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":11,\"targetNodeKey\":\"a\",\"comment\":\"重新核对\"}"))
                .andExpect(status().isOk());
        var param = ArgumentCaptor.forClass(FlowTaskReturnParam.class);
        verify(service).returnTask(param.capture());
        assertEquals(11L, param.getValue().getTaskId());
        assertEquals("a", param.getValue().getTargetNodeKey());
        assertEquals("重新核对", param.getValue().getComment());
    }

    @Test
    void shouldRejectMissingTaskTargetAndBlankReason() throws Exception {
        for (String body : List.of(
                "{\"targetNodeKey\":\"a\",\"comment\":\"原因\"}",
                "{\"taskId\":11,\"comment\":\"原因\"}",
                "{\"taskId\":11,\"targetNodeKey\":\"a\",\"comment\":\"   \"}")) {
            mvc.perform(post("/flow/task/return").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
}
