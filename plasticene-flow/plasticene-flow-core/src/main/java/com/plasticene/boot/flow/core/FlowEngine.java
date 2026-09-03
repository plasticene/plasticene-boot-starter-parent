package com.plasticene.boot.flow.core;

import com.plasticene.boot.flow.core.command.FlowCommands;
import com.plasticene.boot.flow.core.command.FlowQueries;
import com.plasticene.boot.flow.core.command.FlowResults;
import com.plasticene.boot.flow.core.model.FlowDefinition;
import com.plasticene.boot.flow.core.model.FlowInstance;
import com.plasticene.boot.flow.core.model.FlowModel;
import com.plasticene.boot.flow.core.model.FlowTask;

import java.util.List;

/**
 * 嵌入式审批流引擎的统一操作入口。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
public interface FlowEngine {

    FlowResults.ModelResult saveModel(FlowCommands.SaveModel command);

    FlowResults.FormDefinitionResult saveFormDefinition(FlowCommands.SaveFormDefinition command);

    FlowResults.DefinitionResult publishModel(FlowCommands.PublishModel command);

    FlowResults.InstanceResult startFlow(FlowCommands.StartFlow command);

    FlowResults.TaskResult approveTask(FlowCommands.ApproveTask command);

    FlowResults.TaskResult rejectTask(FlowCommands.RejectTask command);

    FlowResults.TaskResult returnTask(FlowCommands.ReturnTask command);

    FlowResults.TaskResult transferTask(FlowCommands.TransferTask command);

    FlowResults.InstanceResult withdrawFlow(FlowCommands.WithdrawFlow command);

    FlowResults.InstanceResult cancelFlow(FlowCommands.CancelFlow command);

    List<FlowModel> findModels(FlowQueries.ModelList query);

    List<FlowDefinition> findDefinitions(FlowQueries.DefinitionList query);

    List<FlowInstance> findMyStartedFlows(FlowQueries.MyStartedFlows query);

    List<FlowTask> findMyTasks(FlowQueries.MyTasks query);

    FlowResults.InstanceDetails getInstanceDetails(FlowQueries.InstanceDetails query);
}
