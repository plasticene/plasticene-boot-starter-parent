package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.FlowEngine;
import com.plasticene.boot.flow.core.FlowEngineImpl;
import com.plasticene.boot.flow.core.dao.FlowFormChangeRecordDAO;
import com.plasticene.boot.flow.core.dao.FlowTaskAddSignDAO;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.dao.FlowTimeoutNoticeDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.event.FlowEventPublisher;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.provider.FlowTaskAssigneeProvider;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.flow.core.service.task.FlowTaskExecution;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 通过真实 Spring 代理和测试事务资源验证任务组件装配及事务边界。
 * @author ZFJ
 * @since 2026-11-10
 */
class FlowTaskTransactionTest {
    private LoginUser loginUserContext;
    private final FlowTaskDAO tasks = mock(FlowTaskDAO.class);
    private final FlowRuntimeService runtime = mock(FlowRuntimeService.class);
    private final FlowDefinitionService definitions = mock(FlowDefinitionService.class);
    private final ProcessExecutor executor = mock(ProcessExecutor.class);
    private final TaskTransactionManager transactions = new TaskTransactionManager();
    private final List<String> committedEvents = new ArrayList<>();
    private AnnotationConfigApplicationContext context;
    private FlowTaskService service;

    @BeforeEach
    void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "task-transaction"), FlowTask.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(FlowTaskDAO.class, () -> tasks);
        context.registerBean(FlowRuntimeService.class, () -> runtime);
        context.registerBean(FlowDefinitionService.class, () -> definitions);
        context.registerBean(ProcessExecutor.class, () -> executor);
        context.registerBean(FlowTaskAddSignDAO.class, () -> mock(FlowTaskAddSignDAO.class));
        context.registerBean(FlowTimeoutNoticeDAO.class, () -> mock(FlowTimeoutNoticeDAO.class));
        context.registerBean(FlowFormChangeRecordDAO.class, () -> mock(FlowFormChangeRecordDAO.class));
        context.registerBean(FlowTaskAssigneeProvider.class, () -> mock(FlowTaskAssigneeProvider.class));
        context.registerBean(FlowOrganizationProvider.class, () -> mock(FlowOrganizationProvider.class));
        context.registerBean(FlowCategoryService.class, () -> mock(FlowCategoryService.class));
        context.registerBean("transactionManager", TaskTransactionManager.class, () -> transactions);
        context.registerBean(FlowEventPublisher.class, () -> event -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    committedEvents.add("completed");
                }
            });
        });
        context.register(TaskConfiguration.class);
        context.refresh();
        service = context.getBean(FlowTaskService.class);
        LoginUser user = new LoginUser();
        user.setId(8L);
        user.setOrgId(9L);
        loginUserContext = user;
        FlowTask task = new FlowTask();
        task.setId(11L);
        task.setInstanceId(21L);
        task.setOrgId(9L);
        task.setAssignee(8L);
        task.setNodeKey("approve");
        task.setNodeType(1);
        task.setApproveMode(1);
        task.setStatus(0);
        task.setIsDelete(0);
        when(tasks.selectById(11L)).thenReturn(task);
        when(tasks.selectList(any())).thenReturn(List.of());
        when(tasks.update(any(FlowTask.class), any())).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            transactions.taskStatus = call.<FlowTask>getArgument(0).getStatus();
            return 1;
        });
        FlowInstance instance = new FlowInstance();
        instance.setId(21L);
        instance.setOrgId(9L);
        instance.setDefinitionId(31L);
        instance.setStatus(0);
        when(runtime.selectInstanceForUpdate(21L)).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return instance;
        });
        FlowDefinition definition = new FlowDefinition();
        FlowNode node = new FlowNode();
        node.setKey("approve");
        definition.setModelNode(node);
        when(definitions.getById(31L)).thenReturn(definition);
    }

    @AfterEach
    void cleanup() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void shouldPreviewRouteThroughTheFacadeWithoutStartingATransaction() {
        var facade = new FlowEngineImpl();
        ReflectionTestUtils.setField(facade, "flowRuntimeService", runtime);
        var datasource = mock(DataSource.class);
        var proxy = new ProxyFactory(facade);
        proxy.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(datasource),
                new AnnotationTransactionAttributeSource()));
        var engine = (FlowEngine) proxy.getProxy();
        when(runtime.calculateRoute(31L, Map.of(), loginUserContext)).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return List.of();
        });

        assertEquals(List.of(), engine.calculateRoute(31L, Map.of(), loginUserContext));
        verifyNoInteractions(datasource);
    }

    @Test
    void shouldKeepLockUpdateAndNodeProgressionInsideEntryTransaction() {
        assertTrue(AopUtils.isAopProxy(service));
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(1, transactions.taskStatus);
            assertEquals(0, transactions.commits);
            return null;
        }).when(executor).moveToNextNode(any(), any());
        service.approveTask(param(), loginUserContext);
        verify(executor).moveToNextNode(any(), any());
        assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, transactions.isolation);
        assertEquals(1, transactions.commits);
        assertEquals(0, transactions.rollbacks);
        assertEquals(List.of("completed"), committedEvents);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    @Test
    void shouldRollbackTaskUpdateWhenNodeProgressionFails() {
        doThrow(new IllegalStateException("节点推进失败")).when(executor).moveToNextNode(any(), any());
        assertThrows(IllegalStateException.class, () -> service.approveTask(param(), loginUserContext));
        assertEquals(0, transactions.taskStatus);
        assertEquals(0, transactions.commits);
        assertEquals(1, transactions.rollbacks);
        assertTrue(committedEvents.isEmpty());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    private FlowTaskParam param() {
        FlowTaskParam param = new FlowTaskParam();
        param.setTaskId(11L);
        return param;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    @ComponentScan(basePackageClasses = FlowTaskExecution.class)
    @Import(FlowTaskServiceImpl.class)
    static class TaskConfiguration {
    }

    /**
     * 用可回滚的内存任务状态观察真实 Spring 事务代理的提交与回滚。
     * @author ZFJ
     * @since 2026-11-10
     */
    static class TaskTransactionManager extends AbstractPlatformTransactionManager {
        private int taskStatus;
        private int snapshot;
        private int isolation;
        private int commits;
        private int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            snapshot = taskStatus;
            isolation = definition.getIsolationLevel();
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            taskStatus = snapshot;
            rollbacks++;
        }
    }
}
