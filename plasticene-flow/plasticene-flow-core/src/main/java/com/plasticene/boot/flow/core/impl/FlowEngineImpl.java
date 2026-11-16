package com.plasticene.boot.flow.core.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.FlowEngine;
import com.plasticene.boot.flow.core.api.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.api.param.FlowFormParam;
import com.plasticene.boot.flow.core.api.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.api.param.FlowModelParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskAddSignParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskFormParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskReturnParam;
import com.plasticene.boot.flow.core.api.param.FlowTaskTransferParam;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.validation.group.Insert;
import com.plasticene.boot.flow.core.api.validation.group.Update;
import com.plasticene.boot.flow.core.api.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.api.vo.FlowFormVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowModelVO;
import com.plasticene.boot.flow.core.api.vo.FlowRouteNodeVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskReturnTargetVO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowModel;
import com.plasticene.boot.flow.core.impl.service.category.FlowCategoryService;
import com.plasticene.boot.flow.core.impl.service.form.FlowFormService;
import com.plasticene.boot.flow.core.impl.service.instance.FlowRuntimeService;
import com.plasticene.boot.flow.core.impl.service.model.FlowModelService;
import com.plasticene.boot.flow.core.impl.service.task.FlowTaskService;
import com.plasticene.boot.flow.core.impl.support.FlowActors;
import jakarta.annotation.Resource;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
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
    @Resource
    private FlowCategoryService flowCategoryService;
    @Resource
    private FlowFormService flowFormService;
    @Resource
    private FlowModelService flowModelService;
    @Resource
    private Validator validator;

    /** 创建流程分类。 */
    @Override
    public Long createCategory(FlowCategoryParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Insert.class);
        return flowCategoryService.createCategory(param, actor);
    }

    /** 修改流程分类。 */
    @Override
    public void updateCategory(FlowCategoryParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Update.class);
        flowCategoryService.updateCategory(param, actor);
    }

    /** 删除单个流程分类。 */
    @Override
    public void deleteCategory(Long categoryId, LoginUser actor) {
        FlowActors.require(actor);
        requireId(categoryId);
        flowCategoryService.delete(List.of(categoryId), actor);
    }

    /** 查询当前组织的流程分类。 */
    @Override
    public List<FlowCategoryVO> listCategories(LoginUser actor) {
        FlowActors.require(actor);
        return new ArrayList<>(flowCategoryService.listCategory(actor));
    }

    /** 创建表单模板。 */
    @Override
    public Long createForm(FlowFormParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Insert.class);
        return flowFormService.create(param, actor);
    }

    /** 修改当前组织的表单模板。 */
    @Override
    public void updateForm(FlowFormParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Update.class);
        flowFormService.update(param, actor);
    }

    /** 删除当前组织的单个表单模板。 */
    @Override
    public void deleteForm(Long formId, LoginUser actor) {
        FlowActors.require(actor);
        requireId(formId);
        flowFormService.delete(formId, actor);
    }

    /** 查询当前组织的表单模板详情。 */
    @Override
    public FlowFormVO getForm(Long formId, LoginUser actor) {
        FlowActors.require(actor);
        requireId(formId);
        return flowFormService.detail(formId, actor);
    }

    /** 查询当前组织的表单模板。 */
    @Override
    public List<FlowFormVO> listForms(LoginUser actor) {
        FlowActors.require(actor);
        return new ArrayList<>(flowFormService.listAll(actor));
    }

    /** 创建流程模型。 */
    @Override
    public Long createModel(FlowModelParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Insert.class);
        return flowModelService.create(param, actor);
    }

    /** 修改流程模型。 */
    @Override
    public void updateModel(FlowModelParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Update.class);
        flowModelService.update(param, actor);
    }

    /** 删除单个流程模型并沿用现有版本保留规则。 */
    @Override
    public void deleteModel(Long modelId, LoginUser actor) {
        FlowActors.require(actor);
        requireId(modelId);
        flowModelService.delete(List.of(modelId), actor);
    }

    /** 查询当前组织的流程模型详情。 */
    @Override
    public FlowModelVO getModel(Long modelId, LoginUser actor) {
        FlowActors.require(actor);
        requireId(modelId);
        return flowModelService.detail(modelId, actor);
    }

    /** 发布流程模型。 */
    @Override
    public void publishModel(Long modelId, LoginUser actor) {
        FlowActors.require(actor);
        requireId(modelId);
        flowModelService.publish(modelId, actor);
    }

    /** 启用或停用已发布的流程模型。 */
    @Override
    public void setModelStatus(FlowModelEnableParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Default.class);
        flowModelService.enable(param, actor);
    }

    @Override
    public Long startInstance(Long definitionId, Map<String, Object> variables, LoginUser actor) {
        FlowActors.require(actor);
        requireId(definitionId);
        return flowRuntimeService.startInstance(definitionId, variables, actor);
    }

    /**
     * 按发布定义 ID发起流程，重复业务申请记录报错，返回新建的实例 ID。
     * @param definitionId 流程发布定义 ID
     * @param businessId 必填的业务申请记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建的实例 ID
     */
    @Override
    public Long startInstanceById(Long definitionId, String businessId, LoginUser actor) {
        return startInstanceById(definitionId, businessId, null, actor);
    }

    /**
     * 按发布定义 ID发起流程，重复业务申请记录报错，返回新建的实例 ID。
     * @param definitionId 流程发布定义 ID
     * @param businessId 必填的业务申请记录 ID
     * @param varMap 审批变量；可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建的实例 ID
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
     * 按稳定流程编码发起流程，重复业务申请记录报错，返回新建的实例 ID。
     * @param processCode 流程稳定流程编码
     * @param businessId 必填的业务申请记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建的实例 ID
     */
    @Override
    public Long startInstanceByCode(String processCode, String businessId, LoginUser actor) {
        return startInstanceByCode(processCode, businessId, null, actor);
    }

    /**
     * 按稳定流程编码发起流程，重复业务申请记录报错，返回新建的实例 ID。
     * @param processCode 流程稳定流程编码
     * @param businessId 必填的业务申请记录 ID
     * @param varMap 审批变量；可为空
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 新建的实例 ID
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
        FlowActors.require(actor);
        validate(param, Default.class);
        flowTaskService.approveTask(param, actor);
    }

    /**
     * 校验操作者的任务办理权限，在实例锁内拒绝申请并结束流程。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void rejectTask(FlowTaskParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Default.class);
        flowTaskService.rejectTask(param, actor);
    }

    /**
     * 校验操作者的任务办理权限，仅保存允许编辑的表单字段并记录变更，不推进审批。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void saveTaskForm(FlowTaskFormParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Default.class);
        flowTaskService.saveForm(param, actor);
    }

    /**
     * 校验操作者的任务办理权限，在实例锁内发起前加签、后加签或并加签。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void addSign(FlowTaskAddSignParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Default.class);
        flowTaskService.addSign(param, actor);
    }

    /**
     * 校验操作者的任务办理权限，将待办转交给同组织的有效用户并保留原办理记录。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void transferTask(FlowTaskTransferParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Default.class);
        flowTaskService.transferTask(param, actor);
    }

    /**
     * 校验操作者的任务办理权限，退回指定前序审批节点并重建待办。
     * @param param 操作参数
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void returnTask(FlowTaskReturnParam param, LoginUser actor) {
        FlowActors.require(actor);
        validate(param, Default.class);
        flowTaskService.returnTask(param, actor);
    }

    /**
     * 校验操作者的撤回权限，撤回其已同意的审批结果并恢复相应待办。
     * @param id 记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void withdrawTask(Long id, LoginUser actor) {
        FlowActors.require(actor);
        requireId(id);
        flowTaskService.withdrawTask(id, actor);
    }

    /**
     * 校验操作者为申请人及流程允许撤销后，取消运行中的实例并发布结束事件。
     * @param id 记录 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     */
    @Override
    public void cancelInstance(Long id, LoginUser actor) {
        FlowActors.require(actor);
        requireId(id);
        flowRuntimeService.cancelInstance(id, actor);
    }

    /**
     * 校验操作者的任务办理权限，查询当前待办可退回的前序人工审批节点。
     * @param taskId 任务 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 可退回的前序人工审批节点
     */
    @Override
    public List<FlowTaskReturnTargetVO> listReturnTargets(Long taskId, LoginUser actor) {
        FlowActors.require(actor);
        requireId(taskId);
        return flowTaskService.listReturnTargets(taskId, actor);
    }

    /**
     * 校验实例组织归属，按操作者和可选任务计算表单读写权限并返回实例详情。
     * @param instanceId 流程实例 ID
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 按操作者权限过滤后的实例详情
     */
    @Override
    public FlowInstanceDetailVO getInstance(Long instanceId, LoginUser actor) {
        return getInstance(instanceId, null, actor);
    }

    @Override
    public FlowInstanceDetailVO getInstance(Long instanceId, Long taskId, LoginUser actor) {
        FlowActors.require(actor);
        requireId(instanceId);
        return flowRuntimeService.getInstanceDetail(instanceId, taskId, actor);
    }


    /**
     * 分页查询操作者本人在所属组织内发起的流程实例。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 本人发起的流程实例分页结果
     */
    @Override
    public PageResult<FlowInstanceVO> pageMyInstances(FlowInstanceQuery query, LoginUser actor) {
        FlowActors.require(actor);
        validate(query, Default.class);
        FlowInstanceQuery scoped = new FlowInstanceQuery();
        BeanUtils.copyProperties(query, scoped);
        scoped.setOrgId(actor.getOrgId());
        scoped.setUserId(actor.getId());
        scoped.setStartUserId(actor.getId());
        return flowRuntimeService.page(scoped, actor);
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
        FlowActors.require(actor);
        requireId(definitionId);
        return flowRuntimeService.calculateRoute(definitionId, variables, actor);
    }

    /**
     * 分页查询操作者所属组织中分配给本人的待办任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query, LoginUser actor) {
        FlowActors.require(actor);
        validate(query, Default.class);
        return flowTaskService.pageMyTodo(query, actor);
    }

    /**
     * 分页查询操作者所属组织中本人已处理的任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query, LoginUser actor) {
        FlowActors.require(actor);
        validate(query, Default.class);
        return flowTaskService.pageMyDone(query, actor);
    }

    /**
     * 分页查询操作者所属组织中抄送给本人的任务。
     * @param query 查询条件
     * @param actor 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query, LoginUser actor) {
        FlowActors.require(actor);
        validate(query, Default.class);
        return flowTaskService.pageMyCopy(query, actor);
    }

    private void validate(Object param, Class<?> group) {
        if (param == null) {
            throw new BizException("参数不能为空");
        }
        var violations = validator.validate(param, group, Default.class);
        if (!violations.isEmpty()) {
            String message = violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted().findFirst().orElseThrow();
            throw new BizException(message);
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BizException("ID不能为空");
        }
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
