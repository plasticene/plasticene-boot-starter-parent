package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.CategoryDAO;
import com.plasticene.boot.flow.core.entity.Category;
import com.plasticene.boot.flow.core.model.param.CategoryParam;
import com.plasticene.boot.flow.core.model.vo.CategoryVO;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryDAO, Category> implements CategoryService {
    @Resource
    private CategoryDAO categoryDAO;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createCategory(CategoryParam param) {
        Category category = PtcBeanUtils.copy(param, Category.class);
        LoginUser loginUser = LoginUserHolder.get();
        category.setOrgId(loginUser.getOrgId());
        categoryDAO.insert(category);
        return category.getId();
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCategory(CategoryParam param) {
        Category category = PtcBeanUtils.copy(param, Category.class);
        categoryDAO.updateById(category);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void sortCategory(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Category> categoryList = new ArrayList<>();
        int seq = 1;
        for (Long id : ids) {
            Category category = new Category();
            category.setId(id);
            category.setSeq(seq++);
            categoryList.add(category);
        }
        categoryDAO.updateById(categoryList);
    }

    @Override
    public List<CategoryVO> listCategory() {
        LoginUser loginUser = LoginUserHolder.get();
        PtcLambdaQueryWrapper<Category> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(Category::getOrgId, loginUser.getOrgId());
        queryWrapper.orderByAsc(Category::getSeq).orderByDesc(Category::getId);
        List<Category> categoryList = categoryDAO.selectList(queryWrapper);
        return PtcBeanUtils.copyList(categoryList, CategoryVO.class);
    }
}
