package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTaskAddSign;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.model.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.model.vo.FlowTaskReturnTargetVO;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowTaskService extends IService<FlowTask> {

    /**
     * 分页查询当前用户待办任务
     */
    PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query);

    /**
     * 分页查询当前用户已办任务
     */
    PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query);

    /**
     * 分页查询抄送给当前用户的任务
     */
    PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query);

    /**
     * 创建开始节点任务
     */
    void createStartTask(FlowInstance instance, FlowNode currentNode);

    /**
     * 创建审批节点任务
     */
    void createApproveTask(FlowInstance instance, FlowNode currentNode);

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
     */
    void approveTask(FlowTaskParam param);

    /**
     * 审批任务
     */
    void approveTask(FlowTaskParam param, LoginUser loginUser);

    /**
     * 拒绝任务
     */
    void rejectTask(FlowTaskParam param);

    /**
     * 拒绝任务
     */
    void rejectTask(FlowTaskParam param, LoginUser loginUser);

    /**
     * 撤回已同意的审批结果
     * @param taskId 任务id
     */
    void withdrawTask(Long taskId);

    /** 对当前待办发起前加签、后加签或并加签。 */
    void addSign(FlowTaskAddSignParam param);

    /** 判断任务是否属于只能审批的前后加签组。 */
    boolean isAddSignGroupTask(FlowTask task);

    /** 查询实例加签记录；调用方须先校验实例组织权限。 */
    List<FlowTaskAddSign> listAddSigns(Long instanceId);

    /** 将当前待办转交给同组织有效用户，保留原任务记录。 */
    void transferTask(FlowTaskTransferParam param);

    /** 查询本人待办可退回的前序人工审批节点。 */
    List<FlowTaskReturnTargetVO> listReturnTargets(Long taskId);

    /** 退回并重新创建目标审批节点待办。 */
    void returnTask(FlowTaskReturnParam param);

    /**
     * 判断当前用户是否可以撤回任务
     */
    boolean canWithdrawTask(FlowTask task, FlowInstance instance,
                            FlowDefinition definition, List<FlowTask> instanceTasks);

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
