package com.plasticene.boot.flow.core.impl.service.definition;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowDefinition;
import com.plasticene.boot.flow.core.impl.model.vo.FlowDefinitionVO;
import com.plasticene.boot.flow.core.impl.model.vo.FlowStartCatalogVO;
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
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    FlowStartCatalogVO getStartableCatalog(LoginUser loginUser);

    /**
     * 获取发起流程定义详情
     * @param definitionId 流程发布id
     * @return 流程定义
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    FlowDefinitionVO getFlowDefinition(Long definitionId, LoginUser loginUser);

    /**
     * 按读取时的模型状态校验组织、当前生效版本及操作者发起权限，不锁定模型。
     * @param definitionId 流程发布定义 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 已校验且可发起的流程发布定义
     */
    FlowDefinition getStartableDefinition(Long definitionId, LoginUser loginUser);

    /**
     * 查询当前组织中指定模型的发布历史，按版本倒序。
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    List<FlowDefinitionVO> listModelVersions(Long modelId, LoginUser loginUser);

    /**
     * 查询当前组织中指定模型的发布快照，不改变当前生效版本。
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    FlowDefinitionVO getModelVersion(Long modelId, Long definitionId, LoginUser loginUser);
}
