package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.entity.FlowCategory;
import com.plasticene.boot.flow.core.model.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.model.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.model.vo.FlowCategoryVO;

import java.util.List;
import java.util.Map;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
public interface FlowCategoryService extends IService<FlowCategory> {

    Long createCategory(FlowCategoryParam param);

    void updateCategory(FlowCategoryParam param);

    void sortCategory(List<Long> ids);

    List<FlowCategoryVO> listCategory();

    PageResult<FlowCategoryVO> page(FlowCategoryQuery query);

    void delete(List<Long> ids);

    Map<String, String> getCategoryMap();

}
