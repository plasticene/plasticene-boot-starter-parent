package com.plasticene.boot.flow.core.service;

import com.plasticene.boot.flow.core.entity.FlowModel;
import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.model.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.model.query.FlowModelQuery;
import com.plasticene.boot.flow.core.model.vo.FlowModelStatisticsVO;
import com.plasticene.boot.flow.core.model.vo.FlowModelVO;
import com.plasticene.boot.common.pojo.PageResult;
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
     */
    Long create(FlowModelParam param);

    /**
     * 更新工作流-流程模型表
     * @param param 工作流-流程模型表参数
     */
    void update(FlowModelParam param);

    /**
     * 批量删除工作流-流程模型表
     * @param idList 工作流-流程模型表id集合参数
     */
    void delete(List<Long> idList);

    /**
     * 分页查询工作流-流程模型表
     * @param query 查询参数
     * @return pageResult
     */
    PageResult<FlowModelVO> page(FlowModelQuery query);

    /**
     * 查询工作流-流程模型表
     * @param id 工作流-流程模型表id
     * @return FlowModelVO
     */
    FlowModelVO detail(Long id);

    /**
     * 获取流程模型统计信息
     * @return FlowModelStatisticsVO
     */
    FlowModelStatisticsVO statistics(FlowModelQuery query);


    /**
     * 开关流程模型， 启用/禁用，发布状态就是启用状态，只有发布之后才能禁用
     * @param param 模型参数
     */
    void enable(FlowModelEnableParam param);

    /**
     * 发布流程模型
     * @param id 模型id
     */
    void publish(Long id);



}

