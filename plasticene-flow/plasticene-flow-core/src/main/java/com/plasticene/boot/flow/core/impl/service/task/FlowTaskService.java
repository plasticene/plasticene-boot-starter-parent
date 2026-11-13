package com.plasticene.boot.flow.core.impl.service.task;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowInstance;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTask;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowTaskAddSign;
import com.plasticene.boot.flow.core.impl.executor.FlowApproveTaskResult;
import com.plasticene.boot.flow.core.model.FlowNode;
import com.plasticene.boot.flow.core.api.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskReturnTargetVO;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskService extends IService<FlowTask> {

    /**
     * 校验操作者的任务办理权限，仅保存允许编辑的表单字段并记录变更，不推进审批。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void saveForm(FlowTaskFormParam param, LoginUser loginUser);

    /**
     * 分页查询操作者所属组织中分配给本人的待办任务。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query, LoginUser loginUser);

    /**
     * 分页查询操作者所属组织中本人已处理的任务。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query, LoginUser loginUser);

    /**
     * 分页查询操作者所属组织中抄送给本人的任务。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query, LoginUser loginUser);

    /**
     * 创建开始节点任务
     */
    void createStartTask(FlowInstance instance, FlowNode currentNode);

    /**
     * 创建审批节点任务并返回处理结果，不推进流程或结束实例。
     * @return 当前节点的等待、通过或拒绝结果
     */
    FlowApproveTaskResult createApproveTask(FlowInstance instance, FlowNode currentNode);

    /**
     * 创建抄送节点任务
     */
    void createCopyTask(FlowInstance instance, FlowNode currentNode);

    /**
     * 创建结束节点任务
     */
    void createEndTask(FlowInstance instance, FlowNode currentNode);

    /**
     * 创建分支任务
     */
    void createBranchTask(FlowInstance instance, FlowNode currentNode);

    /**
     * 创建条件节点任务
     */
    void createConditionNodeTask(FlowInstance instance, FlowNode conditionNode);

    /**
     * 审批任务
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void approveTask(FlowTaskParam param, LoginUser loginUser);

    /**
     * 拒绝任务
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void rejectTask(FlowTaskParam param, LoginUser loginUser);

    /**
     * 校验操作者的撤回权限，撤回其已同意的审批结果并恢复相应待办。
     * @param taskId 任务 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void withdrawTask(Long taskId, LoginUser loginUser);

    /**
     * 校验操作者的任务办理权限，在实例锁内发起前加签、后加签或并加签。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void addSign(FlowTaskAddSignParam param, LoginUser loginUser);

    /** 判断任务是否属于只能审批的前后加签组。 */
    boolean isAddSignGroupTask(FlowTask task);

    /** 查询实例加签记录；调用方须先校验实例组织权限。 */
    List<FlowTaskAddSign> listAddSigns(Long instanceId);

    /**
     * 校验操作者的任务办理权限，将待办转交给同组织的有效用户并保留原办理记录。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void transferTask(FlowTaskTransferParam param, LoginUser loginUser);

    /**
     * 校验操作者的任务办理权限，查询当前待办可退回的前序人工审批节点。
     * @param taskId 任务 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 可退回的前序人工审批节点
     */
    List<FlowTaskReturnTargetVO> listReturnTargets(Long taskId, LoginUser loginUser);

    /**
     * 校验操作者的任务办理权限，退回指定前序审批节点并重建待办。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void returnTask(FlowTaskReturnParam param, LoginUser loginUser);

    /**
     * 根据操作者身份、流程配置和当前任务状态判断审批结果是否允许撤回。
     * @param task 任务快照
     * @param instance 流程实例快照
     * @param definition 流程发布定义快照
     * @param instanceTasks 实例内当前任务记录
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 当前操作者是否允许撤回该审批结果
     */
    boolean canWithdrawTask(FlowTask task, FlowInstance instance, FlowDefinition definition, List<FlowTask> instanceTasks, LoginUser loginUser);

    /**
     * 累计并行网关跳过的分支数量，不查询汇合结果。
     * @param instanceId 实例id
     * @param nodeKey 并行分支节点key
     * @param count 跳过数量，小于等于零时不更新
     */
    void addCompletedBranches(Long instanceId, String nodeKey, int count);

    /**
     * 并行分支任务完成的分支数量+1
     * @param instanceId 实例id
     * @param nodeKey 并行分支节点key
     * @return 最新完成分支数量
     */
    int incrementCompletedBranchAndGet(Long instanceId, String nodeKey);

    /**
     * 删除流程实例下其他正在执行中的任务.
     * @param instanceId 实例id
     */
    void delInstanceRunningTask(Long instanceId);

    /**
     * 取消流程实例下正在执行中的任务
     * @param instanceId 实例id
     */
    void cancelInstanceRunningTask(Long instanceId);

    /**
     * 获取流程实例下所有任务.
     * @param instanceId 实例id
     * @return 任务列表
     */
    List<FlowTask> listTaskByInstanceId(Long instanceId);
}
