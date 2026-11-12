package com.plasticene.boot.flow.core;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.model.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.model.vo.FlowRouteNodeVO;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.model.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
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
 * {@link FlowRuntimeService}、
 * {@link FlowTaskService}、
 * {@link ProcessExecutor}
 * 等组件，对调用方透明，后续实现细节可以演进而不影响外部调用代码。</p>
 *
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowEngine {

    /**
     * 按发布定义 ID发起流程，重复业务申请记录报错，返回新建的实例 ID。
     * @param definitionId 流程发布定义 ID
     * @param businessId 必填的业务申请记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建或已有的实例 ID
     */
    Long startInstanceById(Long definitionId, String businessId, LoginUser actor);

    /**
     * 按发布定义 ID发起流程，重复业务申请记录报错，返回新建的实例 ID。
     * @param definitionId 流程发布定义 ID
     * @param businessId 必填的业务申请记录 ID
     * @param varMap 审批变量；可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建或已有的实例 ID
     */
    Long startInstanceById(Long definitionId, String businessId, Map<String, Object> varMap, LoginUser actor);

    /**
     * 按稳定流程编码发起流程，重复业务申请记录报错，返回新建的实例 ID。
     * @param processCode 流程稳定流程编码
     * @param businessId 必填的业务申请记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建或已有的实例 ID
     */
    Long startInstanceByCode(String processCode, String businessId, LoginUser actor);

    /**
     * 按稳定流程编码发起流程，重复业务申请记录报错，返回新建的实例 ID。
     * @param processCode 流程稳定流程编码
     * @param businessId 必填的业务申请记录 ID
     * @param varMap 审批变量；可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建或已有的实例 ID
     */
    Long startInstanceByCode(String processCode, String businessId, Map<String, Object> varMap, LoginUser actor);

    /**
     * 校验操作者的任务办理权限，在实例锁内完成审批并推进流程。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    void approveTask(FlowTaskParam param, LoginUser actor);
    /**
     * 校验操作者的任务办理权限，在实例锁内拒绝申请并结束流程。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    void rejectTask(FlowTaskParam param, LoginUser actor);
    /**
     * 校验操作者的任务办理权限，仅保存允许编辑的表单字段并记录变更，不推进审批。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    void saveForm(FlowTaskFormParam param, LoginUser actor);
    /**
     * 校验操作者的任务办理权限，在实例锁内发起前加签、后加签或并加签。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    void addSign(FlowTaskAddSignParam param, LoginUser actor);
    /**
     * 校验操作者的任务办理权限，将待办转交给同组织的有效用户并保留原办理记录。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    void transferTask(FlowTaskTransferParam param, LoginUser actor);
    /**
     * 校验操作者的任务办理权限，退回指定前序审批节点并重建待办。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    void returnTask(FlowTaskReturnParam param, LoginUser actor);
    /**
     * 校验操作者的撤回权限，撤回其已同意的审批结果并恢复相应待办。
     * @param taskId 任务 ID；查询实例详情时可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    void withdrawTask(Long taskId, LoginUser actor);
    /**
     * 校验操作者为申请人及流程允许撤销后，取消运行中的实例并发布结束事件。
     * @param instanceId 流程实例 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    void cancelInstance(Long instanceId, LoginUser actor);
    /**
     * 校验操作者的任务办理权限，查询当前待办可退回的前序人工审批节点。
     * @param taskId 任务 ID；查询实例详情时可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 可退回的前序人工审批节点
     */
    List<FlowTaskReturnTargetVO> listReturnTargets(Long taskId, LoginUser actor);
    /**
     * 校验实例组织归属，按操作者和可选任务计算表单读写权限并返回实例详情。
     * @param instanceId 流程实例 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 按操作者权限过滤后的实例详情
     */
    FlowInstanceDetailVO getInstance(Long instanceId, LoginUser actor);
    /**
     * 分页查询操作者本人在所属组织内发起的流程实例。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 本人发起的流程实例分页结果
     */
    PageResult<FlowInstanceVO> pageInstances(FlowInstanceQuery query, LoginUser actor);
    /**
     * 分页查询操作者所属组织中分配给本人的待办任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query, LoginUser actor);
    /**
     * 分页查询操作者所属组织中本人已处理的任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query, LoginUser actor);
    /**
     * 分页查询操作者所属组织中抄送给本人的任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query, LoginUser actor);
    /**
     * 校验操作者的发起权限后，根据变量计算当前生效定义的预计流转路径。
     * @param definitionId 流程发布定义 ID
     * @param variables 审批变量及表单数据
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 按执行顺序排列的预计路径节点
     */
    List<FlowRouteNodeVO> calculateRoute(Long definitionId, Map<String, Object> variables, LoginUser actor);
}
