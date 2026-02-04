package com.plasticene.boot.flow.core;

import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.flow.core.model.param.FlowInstanceParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;

import java.util.List;
import java.util.Map;

/**
 * <b>工作流引擎顶层核心接口（对外唯一推荐使用的门面）</b>
 *
 * <p>设计目标：
 * <ul>
 *     <li>Starter 场景：业务方仅依赖本接口即可完成流程发起、审批、查询等能力</li>
 *     <li>SDK 场景：可在任意 Spring 应用中注入使用，而无需直接依赖内部 Service/DAO</li>
 * </ul>
 *
 * <p>内部实现基于
 * {@link com.plasticene.boot.flow.core.service.FlowRuntimeService}、
 * {@link com.plasticene.boot.flow.core.service.FlowTaskService}、
 * {@link com.plasticene.boot.flow.core.executor.ProcessExecutor}
 * 等组件，对调用方透明，后续实现细节可以演进而不影响外部调用代码。</p>
 *
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowEngine {

    /**
     * 基于流程模型 id 发起一次流程实例（无业务 id、无变量）
     *
     * @param processId 流程模型 id
     * @return 实例 id
     */
    Long startInstance(Long processId);

    /**
     * 基于流程模型 id 发起一次流程实例（带业务 id）
     *
     * @param processId  流程模型 id
     * @param businessId 业务 id
     * @return 实例 id
     */
    Long startInstance(Long processId, Long businessId);

    /**
     * 基于流程模型 id 发起一次流程实例（带业务 id & 变量）
     *
     * @param processId  流程模型 id
     * @param businessId 业务 id
     * @param varMap     变量 map
     * @return 实例 id
     */
    Long startInstance(Long processId, Long businessId, Map<String, Object> varMap);

    /**
     * 基于参数对象发起实例（便于不同调用方按需扩展字段）
     *
     * @param param 实例发起参数
     * @return 实例 id
     */
    Long startInstance(FlowInstanceParam param);

    /**
     * 审批任务（通过）
     *
     * @param param 任务审批参数
     */
    void approveTask(FlowTaskParam param);

    /**
     * 审批任务（拒绝）
     *
     * @param param 任务审批参数
     */
    void rejectTask(FlowTaskParam param);

    /**
     * 根据实例 id 获取实例详情
     *
     * @param instanceId 实例 id
     * @return 实例
     */
    FlowInstance getInstance(Long instanceId);

    /**
     * 查询实例下的所有任务列表
     *
     * @param instanceId 实例 id
     * @return 任务列表
     */
    List<FlowTask> listTasks(Long instanceId);

    /**
     * 查询流程模型
     *
     * @param processId 流程模型 id
     * @return 流程模型
     */
    FlowProcess getProcess(Long processId);

    /**
     * 预计算某个流程在给定变量下的“理论流转路径”
     * <p>常用于：在发起前给前端展示“将经过哪些节点、谁会审批”。</p>
     *
     * @param processId 流程模型 id
     * @param varMap    条件变量
     * @return 将要经过的节点列表（按执行顺序）
     */
    List<ProcessNode> calculateRoute(Long processId, Map<String, Object> varMap);

}
