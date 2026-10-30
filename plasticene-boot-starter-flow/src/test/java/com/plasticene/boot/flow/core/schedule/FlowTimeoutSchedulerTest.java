package com.plasticene.boot.flow.core.schedule;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTimeoutNotice;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.service.FlowTimeoutService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证超时扫描的状态过滤和通知分页处理。
 * @author ZFJ
 * @since 2026-10-30
 */
class FlowTimeoutSchedulerTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldOnlyScanRunningUndeletedDueTasks() {
        initTable(FlowTask.class);
        FlowTaskDAO dao = mock(FlowTaskDAO.class);
        FlowTimeoutService service = mock(FlowTimeoutService.class);
        FlowTask task = new FlowTask();
        task.setId(11L);
        task.setOrgId(9L);
        when(dao.selectList(any())).thenReturn(List.of(task));

        new FlowTimeoutScheduler(dao, service).handleDueTasks();

        ArgumentCaptor<LambdaQueryWrapper<FlowTask>> query = ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(dao).selectList(query.capture());
        String sql = query.getValue().getSqlSegment();
        assertTrue(sql.contains("status ="));
        assertTrue(sql.contains("is_delete ="));
        assertTrue(sql.contains("deadline <="));
        assertTrue(sql.contains("OR next_remind_time <="));
        assertTrue(sql.endsWith("LIMIT 200"));
        assertEquals(FlowTaskStatusEnum.RUNNING.getCode(), query.getValue().getParamNameValuePairs().get("MPGENVAL2"));
        assertEquals(CommonConstant.IS_NOT_DEL, query.getValue().getParamNameValuePairs().get("MPGENVAL3"));
        verify(service).handleDueTask(eq(task), any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldScanPendingNoticesAndContinueAfterFullBatch() {
        initTable(FlowTimeoutNotice.class);
        FlowTimeoutService service = mock(FlowTimeoutService.class);
        List<FlowTimeoutNotice> batch = LongStream.rangeClosed(1, 200).mapToObj(id -> {
            FlowTimeoutNotice notice = new FlowTimeoutNotice();
            notice.setId(id);
            return notice;
        }).toList();
        when(service.list(any(LambdaQueryWrapper.class))).thenReturn(batch, List.of());
        doThrow(new IllegalStateException("send failure")).when(service).sendNotification(eq(1L), any());

        new ReminderScheduler(service).sendNotification();

        ArgumentCaptor<LambdaQueryWrapper<FlowTimeoutNotice>> query = ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(service, times(2)).list(query.capture());
        var first = query.getAllValues().getFirst();
        String sql = first.getSqlSegment();
        assertTrue(sql.contains("state ="));
        assertTrue(sql.contains("retry_at <="));
        assertTrue(sql.endsWith("LIMIT 200"));
        assertTrue(first.getParamNameValuePairs().containsValue("PENDING"));
        var second = query.getAllValues().getLast();
        second.getSqlSegment();
        assertEquals(200L, second.getParamNameValuePairs().get("MPGENVAL1"));
        verify(service, times(200)).sendNotification(anyLong(), any());
    }

    private void initTable(Class<?> entity) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entity);
    }
}
