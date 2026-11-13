package com.plasticene.boot.flow.core.impl.service.category;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowCategory;
import com.plasticene.boot.flow.core.impl.model.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.impl.model.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.impl.model.vo.FlowCategoryVO;
import java.util.List;
import java.util.Map;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
public interface FlowCategoryService extends IService<FlowCategory> {

    /**
     * 在操作者所属组织内创建分类，分类编码不区分大小写且组织内唯一。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 新建记录 ID
     */
    Long createCategory(FlowCategoryParam param, LoginUser loginUser);

    /**
     * 修改操作者所属组织的分类信息，保留创建时的分类编码。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void updateCategory(FlowCategoryParam param, LoginUser loginUser);

    /**
     * 按传入顺序调整操作者所属组织的分类排序。
     * @param ids 待操作的 ID 列表
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void sortCategory(List<Long> ids, LoginUser loginUser);

    /**
     * 查询操作者所属组织的全部流程分类。
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    List<FlowCategoryVO> listCategory(LoginUser loginUser);

    /**
     * 分页查询操作者所属组织的流程分类。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    PageResult<FlowCategoryVO> page(FlowCategoryQuery query, LoginUser loginUser);

    /**
     * 校验分类组织归属、系统标记及引用关系后批量删除分类。
     * @param ids 待操作的 ID 列表
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    void delete(List<Long> ids, LoginUser loginUser);

    /**
     * 查询操作者所属组织的分类编码与名称映射。
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分类编码与名称映射
     */
    Map<String, String> getCategoryMap(LoginUser loginUser);
}
