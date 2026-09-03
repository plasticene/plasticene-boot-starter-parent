package com.plasticene.boot.flow.core.command;

import com.plasticene.boot.flow.core.model.FlowDefinition;
import com.plasticene.boot.flow.core.model.FlowInstance;
import com.plasticene.boot.flow.core.model.FlowModel;
import com.plasticene.boot.flow.core.model.FlowTask;
import com.plasticene.boot.flow.core.model.FormDefinition;
import com.plasticene.boot.flow.core.model.FormInstance;

import java.util.List;

/**
 * 审批流命令执行结果集合。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public final class FlowResults {

    private FlowResults() {
    }

    public record ModelResult(FlowModel model, boolean replayed) {
    }

    public record FormDefinitionResult(FormDefinition formDefinition, boolean replayed) {
    }

    public record DefinitionResult(FlowDefinition definition, boolean replayed) {
    }

    public record InstanceResult(FlowInstance instance, boolean replayed) {
    }

    public record TaskResult(FlowTask task, FlowInstance instance, boolean replayed) {
    }

    public record InstanceDetails(FlowInstance instance, FlowDefinition definition,
                                  FormInstance formInstance, List<FlowTask> tasks) {
        public InstanceDetails {
            tasks = tasks == null ? List.of() : List.copyOf(tasks);
        }
    }
}
