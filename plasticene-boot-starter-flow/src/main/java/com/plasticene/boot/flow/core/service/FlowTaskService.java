package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.model.param.FlowTaskParam;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;

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
     * 拒绝任务
     */
    void rejectTask(FlowTaskParam param);

    /**
     * 后台超时执行入口，调用方必须持有实例锁并校验待办截止时间。
     * */
    default void executeTimeout(FlowTask task) {
        throw new UnsupportedOperationException("当前任务服务未实现超时处理");
    }

    /**
     * 撤回已同意的审批结果
     * @param taskId 任务id
     */
    void withdrawTask(Long taskId);

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
