package com.plasticene.boot.flow.web;

import com.plasticene.boot.flow.core.FlowEngine;
import com.plasticene.boot.flow.core.command.FlowCommands;
import com.plasticene.boot.flow.core.command.FlowQueries;
import com.plasticene.boot.flow.core.command.FlowResults;
import com.plasticene.boot.flow.core.model.FlowActor;
import com.plasticene.boot.flow.core.model.FlowDefinition;
import com.plasticene.boot.flow.core.model.FlowEnums;
import com.plasticene.boot.flow.core.model.FlowInstance;
import com.plasticene.boot.flow.core.model.FlowModel;
import com.plasticene.boot.flow.core.model.FlowTask;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 可选启用的审批流 HTTP 接口。
 *
 * @author ZFJ
 * @since 2026-09-03
 */
@RestController
@RequestMapping("/flow")
public class FlowController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final FlowEngine flowEngine;
    private final FlowWebActorResolver actorResolver;

    public FlowController(FlowEngine flowEngine, FlowWebActorResolver actorResolver) {
        this.flowEngine = flowEngine;
        this.actorResolver = actorResolver;
    }

    @PostMapping("/models")
    public FlowResults.ModelResult saveModel(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                             @RequestBody FlowWebRequests.SaveModel body,
                                             HttpServletRequest request) {
        return flowEngine.saveModel(new FlowCommands.SaveModel(requestId, actor(request), body.modelId(),
                body.code(), body.name(), body.formDefinitionId(), body.rootNode()));
    }

    @GetMapping("/models")
    public List<FlowModel> findModels(@RequestParam(name = "status", required = false) FlowEnums.ModelStatus status,
                                     @RequestParam(name = "offset", defaultValue = "0") int offset,
                                     @RequestParam(name = "limit", defaultValue = "20") int limit,
                                     HttpServletRequest request) {
        return flowEngine.findModels(new FlowQueries.ModelList(actor(request), status, offset, limit));
    }

    @PostMapping("/models/{modelId}/publish")
    public FlowResults.DefinitionResult publishModel(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                                     @PathVariable("modelId") Long modelId,
                                                     HttpServletRequest request) {
        return flowEngine.publishModel(new FlowCommands.PublishModel(requestId, actor(request), modelId));
    }

    @GetMapping("/models/{modelId}/definitions")
    public List<FlowDefinition> findDefinitions(@PathVariable("modelId") Long modelId,
                                                @RequestParam(name = "offset", defaultValue = "0") int offset,
                                                @RequestParam(name = "limit", defaultValue = "20") int limit,
                                                HttpServletRequest request) {
        return flowEngine.findDefinitions(new FlowQueries.DefinitionList(actor(request), modelId, offset, limit));
    }

    @PostMapping("/forms")
    public FlowResults.FormDefinitionResult saveFormDefinition(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                                               @RequestBody FlowWebRequests.SaveFormDefinition body,
                                                               HttpServletRequest request) {
        return flowEngine.saveFormDefinition(new FlowCommands.SaveFormDefinition(requestId, actor(request),
                body.formDefinitionId(), body.code(), body.name(), body.schema()));
    }

    @PostMapping("/instances")
    public FlowResults.InstanceResult startFlow(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                                @RequestBody FlowWebRequests.StartFlow body,
                                                HttpServletRequest request) {
        return flowEngine.startFlow(new FlowCommands.StartFlow(requestId, actor(request), body.modelCode(),
                body.businessKey(), body.variables(), body.formData()));
    }

    @GetMapping("/instances/mine")
    public List<FlowInstance> findMyStartedFlows(
                                                @RequestParam(name = "status", required = false)
                                                FlowEnums.InstanceStatus status,
                                                @RequestParam(name = "offset", defaultValue = "0") int offset,
                                                @RequestParam(name = "limit", defaultValue = "20") int limit,
                                                HttpServletRequest request) {
        return flowEngine.findMyStartedFlows(new FlowQueries.MyStartedFlows(actor(request), status, offset, limit));
    }

    @GetMapping("/instances/{instanceId}")
    public FlowResults.InstanceDetails getInstanceDetails(@PathVariable("instanceId") Long instanceId,
                                                          HttpServletRequest request) {
        return flowEngine.getInstanceDetails(new FlowQueries.InstanceDetails(actor(request), instanceId));
    }

    @PostMapping("/instances/{instanceId}/withdraw")
    public FlowResults.InstanceResult withdrawFlow(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                                   @PathVariable("instanceId") Long instanceId,
                                                   HttpServletRequest request) {
        return flowEngine.withdrawFlow(new FlowCommands.WithdrawFlow(requestId, actor(request), instanceId));
    }

    @PostMapping("/instances/{instanceId}/cancel")
    public FlowResults.InstanceResult cancelFlow(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                                 @PathVariable("instanceId") Long instanceId,
                                                 @RequestBody FlowWebRequests.CancelFlow body,
                                                 HttpServletRequest request) {
        return flowEngine.cancelFlow(new FlowCommands.CancelFlow(requestId, actor(request), instanceId,
                body.reason()));
    }

    @GetMapping("/tasks/mine")
    public List<FlowTask> findMyTasks(@RequestParam(name = "status", required = false) FlowEnums.TaskStatus status,
                                     @RequestParam(name = "offset", defaultValue = "0") int offset,
                                     @RequestParam(name = "limit", defaultValue = "20") int limit,
                                     HttpServletRequest request) {
        return flowEngine.findMyTasks(new FlowQueries.MyTasks(actor(request), status, offset, limit));
    }

    @PostMapping("/tasks/{taskId}/approve")
    public FlowResults.TaskResult approveTask(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                              @PathVariable("taskId") Long taskId,
                                              @RequestBody(required = false) FlowWebRequests.TaskComment body,
                                              HttpServletRequest request) {
        return flowEngine.approveTask(new FlowCommands.ApproveTask(requestId, actor(request), taskId,
                body == null ? null : body.comment()));
    }

    @PostMapping("/tasks/{taskId}/reject")
    public FlowResults.TaskResult rejectTask(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                             @PathVariable("taskId") Long taskId,
                                             @RequestBody(required = false) FlowWebRequests.TaskComment body,
                                             HttpServletRequest request) {
        return flowEngine.rejectTask(new FlowCommands.RejectTask(requestId, actor(request), taskId,
                body == null ? null : body.comment()));
    }

    @PostMapping("/tasks/{taskId}/return")
    public FlowResults.TaskResult returnTask(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                             @PathVariable("taskId") Long taskId,
                                             @RequestBody FlowWebRequests.ReturnTask body,
                                             HttpServletRequest request) {
        return flowEngine.returnTask(new FlowCommands.ReturnTask(requestId, actor(request), taskId,
                body.targetNodeKey(), body.comment()));
    }

    @PostMapping("/tasks/{taskId}/transfer")
    public FlowResults.TaskResult transferTask(@RequestHeader(IDEMPOTENCY_KEY) String requestId,
                                               @PathVariable("taskId") Long taskId,
                                               @RequestBody FlowWebRequests.TransferTask body,
                                               HttpServletRequest request) {
        return flowEngine.transferTask(new FlowCommands.TransferTask(requestId, actor(request), taskId,
                body.newAssigneeId(), body.comment()));
    }

    private FlowActor actor(HttpServletRequest request) {
        return actorResolver.resolve(request);
    }
}
