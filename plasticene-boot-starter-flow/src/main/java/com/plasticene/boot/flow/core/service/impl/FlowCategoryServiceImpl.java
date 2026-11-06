package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowCategoryDAO;
import com.plasticene.boot.flow.core.entity.FlowCategory;
import com.plasticene.boot.flow.core.model.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.model.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.model.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@Service
public class FlowCategoryServiceImpl extends ServiceImpl<FlowCategoryDAO, FlowCategory> implements FlowCategoryService {
    @Resource
    private FlowCategoryDAO flowCategoryDAO;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createCategory(FlowCategoryParam param) {
        FlowCategory category = PtcBeanUtils.copy(param, FlowCategory.class);
        LoginUser loginUser = LoginUserHolder.get();
        category.setOrgId(loginUser.getOrgId());
        category.setId(null);
        category.setIsSys(CommonConstant.IS_OFF);
        if (flowCategoryDAO.countByCode(category.getOrgId(), category.getCode()) > 0) {
            throw new BizException("分类标识已存在");
        }
        try {
            flowCategoryDAO.insert(category);
        } catch (DuplicateKeyException exception) {
            throw new BizException("分类标识已存在");
        }
        return category.getId();
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCategory(FlowCategoryParam param) {
        FlowCategory category = lockCategory(param.getId());
        if (param.getCode() != null && !Objects.equals(param.getCode(), category.getCode())) {
            throw new BizException("分类标识创建后不可修改");
        }
        LambdaUpdateWrapper<FlowCategory> update = new LambdaUpdateWrapper<>();
        update.eq(FlowCategory::getId, category.getId()).eq(FlowCategory::getOrgId, category.getOrgId())
                .set(FlowCategory::getName, param.getName())
                .set(FlowCategory::getIcon, param.getIcon())
                .set(FlowCategory::getDescription, param.getDescription());
        // 显式设置描述，保证 null 和空字符串均可清空旧值；实体触发审计字段更新。
        flowCategoryDAO.update(new FlowCategory(), update);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void sortCategory(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<FlowCategory> categoryList = new ArrayList<>();
        for (Long id : ids.stream().distinct().sorted().toList()) {
            lockCategory(id);
        }
        int seq = 1;
        for (Long id : ids) {
            FlowCategory category = new FlowCategory();
            category.setId(id);
            category.setSeq(seq++);
            categoryList.add(category);
        }
        flowCategoryDAO.updateById(categoryList);
    }

    @Override
    public List<FlowCategoryVO> listCategory() {
        LoginUser loginUser = LoginUserHolder.get();
        PtcLambdaQueryWrapper<FlowCategory> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowCategory::getOrgId, loginUser.getOrgId());
        queryWrapper.orderByDesc(FlowCategory::getId);
        List<FlowCategory> categoryList = flowCategoryDAO.selectList(queryWrapper);
        return PtcBeanUtils.copyList(categoryList, FlowCategoryVO.class);
    }

    @Override
    public PageResult<FlowCategoryVO> page(FlowCategoryQuery query) {
        PtcLambdaQueryWrapper<FlowCategory> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowCategory::getOrgId, LoginUserHolder.get().getOrgId());
        queryWrapper.likeIfPresent(FlowCategory::getName, query.getName());
        queryWrapper.eqIfPresent(FlowCategory::getCode, query.getCode());
        queryWrapper.orderByDesc(FlowCategory::getId);
        PageResult<FlowCategory> result = flowCategoryDAO.selectPage(query, queryWrapper);
        List<FlowCategoryVO> voList = PtcBeanUtils.copyList(result.getList(), FlowCategoryVO.class);
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> categoryIds = ids.stream().distinct().sorted().toList();
        List<FlowCategory> categories = new ArrayList<>();
        for (Long id : categoryIds) {
            categories.add(lockCategory(id));
        }
        for (FlowCategory category : categories) {
            if (Objects.equals(category.getIsSys(), 1)) {
                throw new BizException("系统内置分类不能删除");
            }
            checkReferences(category);
        }
        flowCategoryDAO.deleteByIds(categoryIds);
    }

    @Override
    public Map<String, String> getCategoryMap() {
        PtcLambdaQueryWrapper<FlowCategory> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowCategory::getOrgId, LoginUserHolder.get().getOrgId());
        List<FlowCategory> categoryList = flowCategoryDAO.selectList(queryWrapper);
        return categoryList.stream().collect(Collectors.toMap(FlowCategory::getCode, FlowCategory::getName));
    }


    private FlowCategory lockCategory(Long id) {
        FlowCategory category = flowCategoryDAO.selectForUpdate(LoginUserHolder.get().getOrgId(), id);
        if (category == null) {
            throw new BizException("流程分类不存在");
        }
        return category;
    }

    private void checkReferences(FlowCategory category) {
        Long orgId = category.getOrgId();
        String code = category.getCode();
        String prefix = "分类「" + category.getName() + "」";
        if (flowCategoryDAO.findModelReference(orgId, code) != null) {
            throw new BizException(prefix + "已被流程模型引用，不能删除");
        }
        if (flowCategoryDAO.findFormReference(orgId, code) != null) {
            throw new BizException(prefix + "已被流程表单引用，不能删除");
        }
        if (flowCategoryDAO.findDefinitionReference(orgId, code) != null) {
            throw new BizException(prefix + "已被流程发布版本引用，不能删除");
        }
        if (flowCategoryDAO.findInstanceReference(orgId, code) != null) {
            throw new BizException(prefix + "已被流程实例引用，不能删除");
        }
    }
}
