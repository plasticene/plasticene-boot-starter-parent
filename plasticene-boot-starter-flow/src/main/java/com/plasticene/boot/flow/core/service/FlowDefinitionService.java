package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.model.vo.FlowDefinitionVO;
import com.plasticene.boot.flow.core.model.vo.FlowStartCatalogVO;

import java.util.List;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
public interface FlowDefinitionService extends IService<FlowDefinition> {

    /**
     * 获取最大的版本号
     * @param modelId 模型id
     * @return 最大版本号
     */
    int getMaxVersion(Long modelId);

    /**
     * 查询当前用户可发起的流程目录
     * @return 发起流程目录
     */
    FlowStartCatalogVO getStartableCatalog();

    /**
     * 获取发起流程定义详情
     * @param definitionId 流程发布id
     * @return 流程定义
     */
    FlowDefinitionVO getFlowDefinition(Long definitionId);


    /**
     * 校验当前用户的发起权限并返回流程定义
     * @param definitionId 流程发布id
     * @return 可发起的流程定义
     */
    FlowDefinition getStartableDefinition(Long definitionId);

    /**
     * 查询当前组织中指定模型的发布历史，按版本倒序。
     */
    List<FlowDefinitionVO> listModelVersions(Long modelId);

    /**
     * 查询当前组织中指定模型的发布快照，不改变当前生效版本。
     */
    FlowDefinitionVO getModelVersion(Long modelId, Long definitionId);
}
