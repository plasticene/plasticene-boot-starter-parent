package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.api.param.FlowModelParam;
import com.plasticene.boot.flow.core.api.query.FlowModelQuery;
import com.plasticene.boot.flow.core.api.vo.FlowDefinitionVO;
import com.plasticene.boot.flow.core.api.vo.FlowModelStatisticsVO;
import com.plasticene.boot.flow.core.api.vo.FlowModelVO;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;

/**
 * 流程模型草稿、发布版本与启停管理的公开接口。
 * <p>所有方法均校验宿主提供的可信操作者、组织归属及相应权限。</p>
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowModelApi {
    /**
     * 在操作者所属组织创建流程模型草稿。
     *
     * @param param 模型创建参数
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 新建模型 ID
     */
    Long create(FlowModelParam param, LoginUser actor);
    /**
     * 修改模型设计态配置；已发布定义和运行实例保持不变。
     *
     * @param param 模型 ID 及待修改的草稿配置
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void update(FlowModelParam param, LoginUser actor);
    /**
     * 批量删除当前组织允许删除的模型；任一模型校验失败时整批回滚。
     *
     * @param ids 待删除的模型 ID 列表
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void delete(List<Long> ids, LoginUser actor);
    /**
     * 查询当前组织的模型详情及设计态配置。
     *
     * @param id 模型 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 模型详情
     */
    FlowModelVO get(Long id, LoginUser actor);
    /**
     * 分页查询当前组织的流程模型。
     *
     * @param query 模型筛选与分页条件
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 模型分页结果
     */
    PageResult<FlowModelVO> page(FlowModelQuery query, LoginUser actor);
    /**
     * 统计当前组织内符合条件的模型数量。
     *
     * @param query 模型筛选条件
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 模型统计结果
     */
    FlowModelStatisticsVO statistics(FlowModelQuery query, LoginUser actor);
    /**
     * 启用或停用当前组织的流程模型。
     *
     * @param param 模型 ID 及目标启用状态
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void setEnabled(FlowModelEnableParam param, LoginUser actor);
    /**
     * 发布模型草稿，保存表单类型和变量定义快照，供后续新申请使用。
     *
     * @param id 待发布的模型 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void publish(Long id, LoginUser actor);
    /**
     * 查询当前组织指定模型的发布版本列表。
     *
     * @param id 模型 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 已发布的定义版本列表
     */
    List<FlowDefinitionVO> listVersions(Long id, LoginUser actor);
    /**
     * 查询指定模型下的一个发布版本，并校验模型与定义的归属关系。
     *
     * @param id 模型 ID
     * @param definitionId 发布定义 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 对应版本的定义快照
     */
    FlowDefinitionVO getVersion(Long id, Long definitionId, LoginUser actor);
    /**
     * 按模型所属组织和流程编码读取宿主业务变量定义，不依赖已保存的表单类型，不保存模型。
     *
     * @param id 模型 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 当前变量定义；发布时另行保存为版本快照
     */
    FlowVariableSchema getVariableSchema(Long id, LoginUser actor);
}
