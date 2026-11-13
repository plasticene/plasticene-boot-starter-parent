package com.plasticene.boot.flow.core.impl.service.task;

import com.plasticene.boot.flow.core.impl.task.FlowTaskAddSignOperations;
import com.plasticene.boot.flow.core.impl.task.FlowTaskApproval;
import com.plasticene.boot.flow.core.impl.task.FlowTaskCreation;
import com.plasticene.boot.flow.core.impl.task.FlowTaskExecution;
import com.plasticene.boot.flow.core.impl.task.FlowTaskLifecycle;
import com.plasticene.boot.flow.core.impl.task.FlowTaskQueries;
import com.plasticene.boot.flow.core.impl.task.FlowTaskReturn;
import com.plasticene.boot.flow.core.impl.task.FlowTaskWithdraw;
import jakarta.annotation.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 为任务行为测试装配真实业务组件并注入可替换的外部依赖。
 * @author ZFJ
 * @since 2026-11-10
 */
final class FlowTaskTestComponents {
    private FlowTaskTestComponents() {
    }

    static void install(FlowTaskServiceImpl service, Object... dependencies) {
        List<Object> components = new ArrayList<>(List.of(
                new FlowTaskExecution(), new FlowTaskQueries(), new FlowTaskCreation(),
                new FlowTaskLifecycle(), new FlowTaskApproval(), new FlowTaskAddSignOperations(),
                new FlowTaskReturn(), new FlowTaskWithdraw()));
        List<Object> candidates = new ArrayList<>(components);
        candidates.addAll(Arrays.asList(dependencies));
        components.add(service);
        for (Object target : components) {
            for (var field : target.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Resource.class)) {
                    candidates.stream().filter(field.getType()::isInstance).findFirst()
                            .ifPresent(value -> ReflectionTestUtils.setField(target, field.getName(), value));
                }
            }
        }
    }

    static void setDependency(FlowTaskServiceImpl service, String name, Object value) {
        boolean matched = false;
        for (var field : FlowTaskServiceImpl.class.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                ReflectionTestUtils.setField(service, name, value);
                matched = true;
            }
            if (field.getType().getPackageName().equals(FlowTaskExecution.class.getPackageName())) {
                Object component = ReflectionTestUtils.getField(service, field.getName());
                for (var dependency : field.getType().getDeclaredFields()) {
                    if (dependency.getName().equals(name)) {
                        ReflectionTestUtils.setField(component, name, value);
                        matched = true;
                    }
                }
            }
        }
        if (!matched) {
            // MyBatis-Plus 的继承字段仍由门面持有。
            ReflectionTestUtils.setField(service, name, value);
        }
    }
}
