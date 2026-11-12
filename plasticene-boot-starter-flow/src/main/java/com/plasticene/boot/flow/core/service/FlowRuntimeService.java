package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.model.vo.FlowRouteNodeVO;
import java.util.List;
import java.util.Map;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowRuntimeService extends IService<FlowInstance> {

    /**
     * 供 Web 动态表单发起独立流程，每次调用创建新实例，不关联外部业务申请记录。
     * @param definitionId 流程发布定义 ID
     * @param varMap 审批变量及表单数据
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 新建的实例 ID
     */
    Long startInstance(Long definitionId, Map<String, Object> varMap, LoginUser loginUser);

    /**
     * 按发布定义在当前事务内发起流程；同一组织和流程分类下的业务申请记录不能重复发起。
     * @param definitionId 流程发布定义 ID
     * @param businessId 业务申请记录 ID，按传入值直接插入；Web 独立发起时为 null
     * @param varMap 审批变量及表单数据
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 新建的实例 ID
     */
    Long startInstance(Long definitionId, String businessId, Map<String, Object> varMap, LoginUser loginUser);

    /**
     * 校验操作者的发起权限后，根据变量计算当前生效定义的预计流转路径。
     * @param definitionId 流程发布定义 ID
     * @param varMap 审批变量及表单数据
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 按执行顺序排列的预计路径节点
     */
    List<FlowRouteNodeVO> calculateRoute(Long definitionId, Map<String, Object> varMap, LoginUser loginUser);

    /**
     * 在指定组织查询条件下分页返回流程数据，并使用操作者组织解析关联信息。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    PageResult<FlowInstanceVO> page(FlowInstanceQuery query, LoginUser loginUser);

    /**
     * 统计流程实例状态数量
     * @param query 查询参数，状态条件不参与统计
     * @return 流程实例状态统计
     */
    FlowInstanceStatisticsVO statistics(FlowInstanceQuery query);

    /**
     * 校验实例组织归属，按操作者和可选任务计算表单读写权限并返回实例详情。
     * @param instanceId 流程实例 ID
     * @param taskId 任务 ID；为空时按操作者的实例可读范围返回
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 按操作者权限过滤后的实例详情
     */
    FlowInstanceDetailVO getInstanceDetail(Long instanceId, Long taskId, LoginUser loginUser);

    /**
     * 校验操作者为申请人及流程允许撤销后，取消运行中的实例并发布结束事件。
     * @param instanceId 流程实例 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void cancelInstance(Long instanceId, LoginUser loginUser);

    /**
     * 对实例加行锁，保证并发情况下数据准确性
     * 一般情况不会触发，但是极限同时审批时可能出现下面情况：
     * 1、或签：两个人极限情况下同时审批时，应该以第一个人的审批为准，但是可能两个人都审批了导致错误
     * 2、会签：两个人极限情况下同时审批时，判断是否都审批过无法准确判断，导致流程没法正常流转
     * @param instanceId 实例id
     * @return 实例信息
     */
    FlowInstance selectInstanceForUpdate(Long instanceId);

    /**
     * 更新流程实例执行到的当前节点
     * @param instanceId 实例id
     * @param currentNode 当前节点
     */
    void updateInstanceCurrentNode(Long instanceId, FlowNode currentNode);

    /**
     * 结束流程实例；拒绝时先清理剩余运行任务，再更新实例并发布结束事件。
     * @param instance 实例
     * @param currentNode 当前节点
     * @param status 实例状态
     */
    void endInstance(FlowInstance instance, FlowNode currentNode, FlowInstanceStatusEnum status);
}
