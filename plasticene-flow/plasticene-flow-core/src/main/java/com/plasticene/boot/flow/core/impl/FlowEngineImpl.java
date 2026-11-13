package com.plasticene.boot.flow.core.impl;

import com.plasticene.boot.flow.core.api.FlowEngine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.flow.core.api.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowRouteNodeVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * {@link FlowEngine} 默认实现，基于内部 Service/Executor 进行封装。
 *
 * <p>推荐业务方仅依赖本实现对外暴露的接口，避免直接耦合内部 Service，
 * 便于后续引擎实现的演进与扩展。</p>
 *
 * @author ZFJ
 * @date 2025/9/2
 */
@Service
public class FlowEngineImpl implements FlowEngine {

    @Resource
    private FlowRuntimeService flowRuntimeService;
    @Resource
    private FlowTaskService flowTaskService;
    @Resource
    private FlowModelDAO flowModelDAO;

    /**
     * 按发布定义 ID幂等发起流程，返回新建或已有的实例 ID。
     * @param definitionId 流程发布定义 ID
     * @param businessId 必填的业务申请记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建或已有的实例 ID
     */
    @Override
    public Long startInstanceById(Long definitionId, String businessId, LoginUser actor) {
        return startInstanceById(definitionId, businessId, null, actor);
    }

    /**
     * 按发布定义 ID幂等发起流程，返回新建或已有的实例 ID。
     * @param definitionId 流程发布定义 ID
     * @param businessId 必填的业务申请记录 ID
     * @param varMap 审批变量；可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建或已有的实例 ID
     */
    @Override
    public Long startInstanceById(Long definitionId, String businessId, Map<String, Object> varMap, LoginUser actor) {
        FlowActors.require(actor);
        requireBusinessId(businessId);
        if (definitionId == null) {
            throw new BizException("流程发布定义ID不能为空");
        }
        return flowRuntimeService.startInstance(definitionId, businessId, varMap, actor);
    }

    /**
     * 按稳定流程编码幂等发起流程，返回新建或已有的实例 ID。
     * @param processCode 流程稳定流程编码
     * @param businessId 必填的业务申请记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建或已有的实例 ID
     */
    @Override
    public Long startInstanceByCode(String processCode, String businessId, LoginUser actor) {
        return startInstanceByCode(processCode, businessId, null, actor);
    }

    /**
     * 按稳定流程编码幂等发起流程，返回新建或已有的实例 ID。
     * @param processCode 流程稳定流程编码
     * @param businessId 必填的业务申请记录 ID
     * @param varMap 审批变量；可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建或已有的实例 ID
     */
    @Override
    public Long startInstanceByCode(String processCode, String businessId, Map<String, Object> varMap, LoginUser actor) {
        FlowActors.require(actor);
        requireBusinessId(businessId);
        if (processCode == null || processCode.isBlank()) {
            throw new BizException("流程编码不能为空");
        }
        var model = flowModelDAO.selectOne(new LambdaQueryWrapper<FlowModel>()
                .eq(FlowModel::getOrgId, actor.getOrgId())
                .eq(FlowModel::getCode, processCode));
        if (model == null || model.getActiveDefinitionId() == null) {
            throw new BizException("流程不存在或尚未发布");
        }
        return flowRuntimeService.startInstance(model.getActiveDefinitionId(), businessId, varMap, actor);
    }

    /**
     * 校验操作者的任务办理权限，在实例锁内完成审批并推进流程。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void approveTask(FlowTaskParam param, LoginUser actor) {
        flowTaskService.approveTask(param, FlowActors.require(actor));
    }

    /**
     * 校验操作者的任务办理权限，在实例锁内拒绝申请并结束流程。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void rejectTask(FlowTaskParam param, LoginUser actor) {
        flowTaskService.rejectTask(param, FlowActors.require(actor));
    }

    /**
     * 校验操作者的任务办理权限，仅保存允许编辑的表单字段并记录变更，不推进审批。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void saveForm(FlowTaskFormParam param, LoginUser actor) {
        flowTaskService.saveForm(param, FlowActors.require(actor));
    }

    /**
     * 校验操作者的任务办理权限，在实例锁内发起前加签、后加签或并加签。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void addSign(FlowTaskAddSignParam param, LoginUser actor) {
        flowTaskService.addSign(param, FlowActors.require(actor));
    }

    /**
     * 校验操作者的任务办理权限，将待办转交给同组织的有效用户并保留原办理记录。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void transferTask(FlowTaskTransferParam param, LoginUser actor) {
        flowTaskService.transferTask(param, FlowActors.require(actor));
    }

    /**
     * 校验操作者的任务办理权限，退回指定前序审批节点并重建待办。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void returnTask(FlowTaskReturnParam param, LoginUser actor) {
        flowTaskService.returnTask(param, FlowActors.require(actor));
    }

    /**
     * 校验操作者的撤回权限，撤回其已同意的审批结果并恢复相应待办。
     * @param id 记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void withdrawTask(Long id, LoginUser actor) {
        flowTaskService.withdrawTask(id, FlowActors.require(actor));
    }

    /**
     * 校验操作者为申请人及流程允许撤销后，取消运行中的实例并发布结束事件。
     * @param id 记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void cancelInstance(Long id, LoginUser actor) {
        flowRuntimeService.cancelInstance(id, FlowActors.require(actor));
    }

    /**
     * 校验操作者的任务办理权限，查询当前待办可退回的前序人工审批节点。
     * @param taskId 任务 ID；查询实例详情时可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 可退回的前序人工审批节点
     */
    @Override
    public List<FlowTaskReturnTargetVO> listReturnTargets(Long taskId, LoginUser actor) {
        return flowTaskService.listReturnTargets(taskId, FlowActors.require(actor));
    }

    /**
     * 校验实例组织归属，按操作者和可选任务计算表单读写权限并返回实例详情。
     * @param instanceId 流程实例 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 按操作者权限过滤后的实例详情
     */
    @Override
    public FlowInstanceDetailVO getInstance(Long instanceId, LoginUser actor) {
        return flowRuntimeService.getInstanceDetail(instanceId, null, FlowActors.require(actor));
    }

    /**
     * 分页查询操作者本人在所属组织内发起的流程实例。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 本人发起的流程实例分页结果
     */
    @Override
    public PageResult<FlowInstanceVO> pageInstances(FlowInstanceQuery query, LoginUser actor) {
        FlowActors.require(actor);
        query.setOrgId(actor.getOrgId());
        query.setUserId(actor.getId());
        return flowRuntimeService.page(query, actor);
    }

    /**
     * 校验操作者的发起权限后，根据变量计算当前生效定义的预计流转路径。
     * @param definitionId 流程发布定义 ID
     * @param variables 审批变量及表单数据
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 按执行顺序排列的预计路径节点
     */
    @Override
    public List<FlowRouteNodeVO> calculateRoute(Long definitionId, Map<String, Object> variables, LoginUser actor) {
        return flowRuntimeService.calculateRoute(definitionId, variables, FlowActors.require(actor));
    }

    /**
     * 分页查询操作者所属组织中分配给本人的待办任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query, LoginUser actor) {
        return flowTaskService.pageMyTodo(query, FlowActors.require(actor));
    }

    /**
     * 分页查询操作者所属组织中本人已处理的任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query, LoginUser actor) {
        return flowTaskService.pageMyDone(query, FlowActors.require(actor));
    }

    /**
     * 分页查询操作者所属组织中抄送给本人的任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query, LoginUser actor) {
        return flowTaskService.pageMyCopy(query, FlowActors.require(actor));
    }
    /** 校验业务接入必须提供有效申请记录 ID，避免误入独立 Web 发起路径。 */
    private void requireBusinessId(String businessId) {
        if (businessId == null || businessId.isBlank()) {
            throw new BizException("申请记录ID不能为空");
        }
        if (businessId.length() > 100) {
            throw new BizException("申请记录ID不能超过100字");
        }
    }

}
