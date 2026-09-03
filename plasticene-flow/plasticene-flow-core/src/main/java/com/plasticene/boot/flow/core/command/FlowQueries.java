package com.plasticene.boot.flow.core.command;

import com.plasticene.boot.flow.core.model.FlowActor;
import com.plasticene.boot.flow.core.model.FlowEnums;

import java.util.Objects;

/**
 * 审批流分页查询条件集合。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public final class FlowQueries {

    private FlowQueries() {
    }

    public record ModelList(FlowActor actor, FlowEnums.ModelStatus status, int offset, int limit) {
        public ModelList {
            requirePage(actor, offset, limit);
        }
    }

    public record DefinitionList(FlowActor actor, Long modelId, int offset, int limit) {
        public DefinitionList {
            requirePage(actor, offset, limit);
            Objects.requireNonNull(modelId, "modelId");
        }
    }

    public record MyStartedFlows(FlowActor actor, FlowEnums.InstanceStatus status, int offset, int limit) {
        public MyStartedFlows {
            requirePage(actor, offset, limit);
        }
    }

    public record MyTasks(FlowActor actor, FlowEnums.TaskStatus status, int offset, int limit) {
        public MyTasks {
            requirePage(actor, offset, limit);
        }
    }

    public record InstanceDetails(FlowActor actor, Long instanceId) {
        public InstanceDetails {
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(instanceId, "instanceId");
        }
    }

    private static void requirePage(FlowActor actor, int offset, int limit) {
        Objects.requireNonNull(actor, "actor");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("limit must be between 1 and 200");
        }
    }
}
