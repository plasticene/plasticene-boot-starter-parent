package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.model.param.FlowProcessParam;
import com.plasticene.boot.flow.core.model.vo.CategoryVO;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
public interface FlowProcessService extends IService<FlowProcess> {

    /**
     * 创建流程模型
     * @param param 流程模型参数
     * @return 流程模型id
     */
    Long createFlowProcess(FlowProcessParam param);

    /**
     * 修改流程模型
     * @param param 流程模型参数
     */
    Long updateFlowProcess(FlowProcessParam param);

    /**
     * 基于流程id发布  应用于流行模型列表进行发布(先保存成草稿再发布)
     * 只有草稿状态的才能发布
     * @param processId 流程id
     */
    void releaseFlowProcess(Long processId);

    /**
     * 基于流程参数发布，应用于配置好流程模型直接发布
     * 这时候需要实现先保存，再发布
     * @param param 流程模型参数
     */
    void releaseFlowProcess(FlowProcessParam param);

    /**
     * 查询流程列表 基于分组分类返回, 有流程名称搜索返回命中的分组及其流程，没搜索返回所有分组 <br>
     * 一个流程可能三种状态数据：草稿、发布、历史<br>
     * 返回流程数据逻辑：有发布返回发布，没有返回草稿，历史在列表不返回
     * @param processName 根据流程名称左右模糊搜索，支持为空
     * @return 流程列表
     */
    List<CategoryVO> listFlowProcess(@Nullable String processName);





}
