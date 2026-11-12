package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.model.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.model.query.FlowModelQuery;
import com.plasticene.boot.flow.core.model.vo.*;
import java.util.List;

/**
 *
 * <p> 工作流-流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

public interface FlowModelService extends IService<FlowModel> {

    /**
     * 创建工作流-流程模型表
     * @param param 工作流-流程模型表参数
     * @return id
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    Long create(FlowModelParam param, LoginUser loginUser);

    /**
     * 更新工作流-流程模型表
     * @param param 工作流-流程模型表参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void update(FlowModelParam param, LoginUser loginUser);

    /**
     * 批量删除工作流-流程模型表
     * @param idList 工作流-流程模型表id集合参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void delete(List<Long> idList, LoginUser loginUser);

    /**
     * 分页查询工作流-流程模型表
     * @param query 查询参数
     * @return pageResult
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    PageResult<FlowModelVO> page(FlowModelQuery query, LoginUser loginUser);

    /**
     * 查询工作流-流程模型表
     * @param id 工作流-流程模型表id
     * @return FlowModelVO
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    FlowModelVO detail(Long id, LoginUser loginUser);

    /**
     * 获取流程模型统计信息
     * @return FlowModelStatisticsVO
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    FlowModelStatisticsVO statistics(FlowModelQuery query, LoginUser loginUser);

    /**
     * 开关流程模型， 启用/禁用，发布状态就是启用状态，只有发布之后才能禁用
     * @param param 模型参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void enable(FlowModelEnableParam param, LoginUser loginUser);

    /**
     * 发布流程模型
     * @param id 模型id
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void publish(Long id, LoginUser loginUser);



}
