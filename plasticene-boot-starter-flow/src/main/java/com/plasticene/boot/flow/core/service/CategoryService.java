package com.plasticene.boot.flow.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.entity.Category;
import com.plasticene.boot.flow.core.model.param.CategoryParam;
import com.plasticene.boot.flow.core.model.query.CategoryQuery;
import com.plasticene.boot.flow.core.model.vo.CategoryVO;

import java.util.List;
import java.util.Map;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
public interface CategoryService extends IService<Category> {

    Long createCategory(CategoryParam param);

    void updateCategory(CategoryParam param);

    void sortCategory(List<Long> ids);

    List<CategoryVO> listCategory();

    PageResult<CategoryVO> page(CategoryQuery query);

    void delete(List<Long> ids);

    Map<String, String> getCategoryMap();

}
