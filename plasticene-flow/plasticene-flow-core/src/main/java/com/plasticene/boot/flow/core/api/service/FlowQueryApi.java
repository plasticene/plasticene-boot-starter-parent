package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.query.FlowDashboardQuery;
import com.plasticene.boot.flow.core.api.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.api.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.api.vo.FlowDashboardVO;
import com.plasticene.boot.flow.core.api.vo.FlowDefinitionVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.api.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.api.vo.FlowStartCatalogVO;
import com.plasticene.boot.flow.core.api.vo.FlowTaskPageVO;

/**
 * 流程实例、个人任务、发起目录与统计数据的公开查询接口。
 * <p>所有方法均校验宿主提供的可信操作者、组织归属及相应权限。</p>
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowQueryApi {
    /**
     * 分页查询操作者本人在当前组织发起的流程申请。
     *
     * @param query 实例筛选与分页条件；用户和组织范围以操作者为准
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 本人发起的实例分页结果
     */
    PageResult<FlowInstanceVO> pageMyInstances(FlowInstanceQuery query, LoginUser actor);
    /**
     * 分页查询当前组织的流程实例，要求具备组织范围的实例查询权限。
     *
     * @param query 实例筛选与分页条件
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 当前组织的实例分页结果
     */
    PageResult<FlowInstanceVO> pageInstances(FlowInstanceQuery query, LoginUser actor);
    /**
     * 统计当前组织的流程实例，要求具备组织范围的实例查询权限。
     *
     * @param query 实例统计筛选条件
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 实例统计结果
     */
    FlowInstanceStatisticsVO instanceStatistics(FlowInstanceQuery query, LoginUser actor);
    /**
     * 查询当前组织的流程数据总览，要求具备数据总览权限。
     *
     * @param query 时间范围等总览筛选条件
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 流程数据总览
     */
    FlowDashboardVO dashboard(FlowDashboardQuery query, LoginUser actor);
    /**
     * 查询实例审批详情，校验参与范围或组织管理权限，以及任务与实例的关联。
     *
     * @param id 流程实例 ID
     * @param taskId 详情关联的任务 ID，可为空
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 实例、审批记录和可用操作等详情
     */
    FlowInstanceDetailVO getInstance(Long id, Long taskId, LoginUser actor);
    /**
     * 查询当前操作者可发起的流程目录，使用已生效定义的表单类型。
     *
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 按分类组织的可发起流程目录
     */
    FlowStartCatalogVO getStartableCatalog(LoginUser actor);
    /**
     * 查询当前操作者有权发起的指定流程定义。
     *
     * @param id 发布定义 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 可发起的定义快照
     */
    FlowDefinitionVO getStartableDefinition(Long id, LoginUser actor);
    /**
     * 分页查询操作者在当前组织的待办审批任务。
     *
     * @param query 任务筛选与分页条件；用户和组织范围以操作者为准
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 个人待办分页结果
     */
    PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query, LoginUser actor);
    /**
     * 分页查询操作者在当前组织已处理的审批任务。
     *
     * @param query 任务筛选与分页条件；用户和组织范围以操作者为准
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 个人已办分页结果
     */
    PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query, LoginUser actor);
    /**
     * 分页查询当前组织抄送给操作者的任务记录。
     *
     * @param query 任务筛选与分页条件；用户和组织范围以操作者为准
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 个人抄送分页结果
     */
    PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query, LoginUser actor);
}
