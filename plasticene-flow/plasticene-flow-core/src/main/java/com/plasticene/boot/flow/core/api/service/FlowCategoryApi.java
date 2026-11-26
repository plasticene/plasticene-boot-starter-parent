package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.api.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.api.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.api.vo.FlowBusinessFieldsVO;
import java.util.List;

/**
 * 流程分类维护与排序的公开接口。
 * <p>所有方法均校验宿主提供的可信操作者、组织归属及相应权限。</p>
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowCategoryApi {
    /** 查询分类管理可用的审批列表字段能力。 */
    default FlowBusinessFieldsVO.Capabilities fieldsCapabilities(LoginUser actor) {
        return new FlowBusinessFieldsVO.Capabilities(false, null);
    }

    /**
     * 在当前组织创建流程分类，分类编码在组织内唯一。
     *
     * @param param 分类创建参数
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 新建分类 ID
     */
    Long create(FlowCategoryParam param, LoginUser actor);
    /**
     * 修改当前组织的流程分类信息，保留创建时的分类编码。
     *
     * @param param 分类 ID 和待修改的信息
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void update(FlowCategoryParam param, LoginUser actor);
    /**
     * 校验组织归属、系统分类及引用关系后批量删除流程分类。
     *
     * @param ids 待删除的分类 ID 列表
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void delete(List<Long> ids, LoginUser actor);
    /**
     * 查询当前组织的流程分类列表。
     *
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 流程分类列表
     */
    List<FlowCategoryVO> list(LoginUser actor);
    /**
     * 分页查询当前组织的流程分类。
     *
     * @param query 分类筛选与分页条件
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 流程分类分页结果
     */
    PageResult<FlowCategoryVO> page(FlowCategoryQuery query, LoginUser actor);
    /**
     * 按传入顺序更新当前组织的流程分类排序。
     *
     * @param ids 按目标顺序排列的分类 ID 列表
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void sort(List<Long> ids, LoginUser actor);
}
