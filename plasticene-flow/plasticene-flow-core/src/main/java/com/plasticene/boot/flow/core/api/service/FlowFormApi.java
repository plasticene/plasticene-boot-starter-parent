package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.param.FlowFormParam;
import com.plasticene.boot.flow.core.api.query.FlowFormQuery;
import com.plasticene.boot.flow.core.api.vo.FlowFormVO;
import java.util.List;

/**
 * FLOW 自带表单模板管理的公开接口。
 * <p>所有方法均校验宿主提供的可信操作者、组织归属及相应权限。</p>
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowFormApi {
    /**
     * 校验表单配置并在操作者所属组织创建自带表单模板。
     *
     * @param param 表单模板创建参数
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 新建表单模板 ID
     */
    Long create(FlowFormParam param, LoginUser actor);
    /**
     * 修改当前组织的自带表单模板，不修改历史发布定义的表单快照。
     *
     * @param param 表单模板 ID 和待修改的配置
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void update(FlowFormParam param, LoginUser actor);
    /**
     * 批量删除当前组织允许删除的自带表单模板。
     *
     * @param ids 待删除的表单模板 ID 列表
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void delete(List<Long> ids, LoginUser actor);
    /**
     * 查询当前组织的自带表单模板列表。
     *
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 表单模板列表
     */
    List<FlowFormVO> list(LoginUser actor);
    /**
     * 分页查询当前组织的自带表单模板。
     *
     * @param query 表单模板筛选与分页条件
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 表单模板分页结果
     */
    PageResult<FlowFormVO> page(FlowFormQuery query, LoginUser actor);
    /**
     * 查询当前组织的一个自带表单模板及其配置。
     *
     * @param id 表单模板 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 表单模板详情
     */
    FlowFormVO get(Long id, LoginUser actor);
}
