package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowCategoryDAO;
import com.plasticene.boot.flow.core.entity.FlowCategory;
import com.plasticene.boot.flow.core.model.param.FlowCategoryParam;
import com.plasticene.boot.flow.core.model.query.FlowCategoryQuery;
import com.plasticene.boot.flow.core.model.vo.FlowCategoryVO;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ZFJ
 * @date 2025/11/19
 */
@Service
public class FlowCategoryServiceImpl extends ServiceImpl<FlowCategoryDAO, FlowCategory> implements FlowCategoryService {
    @Resource
    private FlowCategoryDAO flowCategoryDAO;

    /**
     * 在操作者所属组织内创建分类，分类编码不区分大小写且组织内唯一。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 新建记录 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createCategory(FlowCategoryParam param, LoginUser loginUser) {
        FlowCategory category = PtcBeanUtils.copy(param, FlowCategory.class);
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

    /**
     * 修改操作者所属组织的分类信息，保留创建时的分类编码。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCategory(FlowCategoryParam param, LoginUser loginUser) {
        FlowCategory category = lockCategory(param.getId(), loginUser);
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

    /**
     * 按传入顺序调整操作者所属组织的分类排序。
     * @param ids 待操作的 ID 列表
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void sortCategory(List<Long> ids, LoginUser loginUser) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<FlowCategory> categoryList = new ArrayList<>();
        for (Long id : ids.stream().distinct().sorted().toList()) {
            lockCategory(id, loginUser);
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

    /**
     * 查询操作者所属组织的全部流程分类。
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    @Override
    public List<FlowCategoryVO> listCategory(LoginUser loginUser) {
        PtcLambdaQueryWrapper<FlowCategory> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowCategory::getOrgId, loginUser.getOrgId());
        queryWrapper.orderByDesc(FlowCategory::getId);
        List<FlowCategory> categoryList = flowCategoryDAO.selectList(queryWrapper);
        return PtcBeanUtils.copyList(categoryList, FlowCategoryVO.class);
    }

    /**
     * 分页查询操作者所属组织的流程分类。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowCategoryVO> page(FlowCategoryQuery query, LoginUser loginUser) {
        PtcLambdaQueryWrapper<FlowCategory> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowCategory::getOrgId, loginUser.getOrgId());
        queryWrapper.likeIfPresent(FlowCategory::getName, query.getName());
        queryWrapper.eqIfPresent(FlowCategory::getCode, query.getCode());
        queryWrapper.orderByDesc(FlowCategory::getId);
        PageResult<FlowCategory> result = flowCategoryDAO.selectPage(query, queryWrapper);
        List<FlowCategoryVO> voList = PtcBeanUtils.copyList(result.getList(), FlowCategoryVO.class);
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    /**
     * 校验分类组织归属、系统标记及引用关系后批量删除分类。
     * @param ids 待操作的 ID 列表
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Long> ids, LoginUser loginUser) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<Long> categoryIds = ids.stream().distinct().sorted().toList();
        List<FlowCategory> categories = new ArrayList<>();
        for (Long id : categoryIds) {
            categories.add(lockCategory(id, loginUser));
        }
        for (FlowCategory category : categories) {
            if (Objects.equals(category.getIsSys(), 1)) {
                throw new BizException("系统内置分类不能删除");
            }
            checkReferences(category);
        }
        flowCategoryDAO.deleteByIds(categoryIds);
    }

    /**
     * 查询操作者所属组织的分类编码与名称映射。
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分类编码与名称映射
     */
    @Override
    public Map<String, String> getCategoryMap(LoginUser loginUser) {
        PtcLambdaQueryWrapper<FlowCategory> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowCategory::getOrgId, loginUser.getOrgId());
        List<FlowCategory> categoryList = flowCategoryDAO.selectList(queryWrapper);
        return categoryList.stream().collect(Collectors.toMap(FlowCategory::getCode, FlowCategory::getName));
    }

    /**
     * 锁定指定分类并校验其归属于操作者所在组织。
     * @param id 记录 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    private FlowCategory lockCategory(Long id, LoginUser loginUser) {
        FlowCategory category = flowCategoryDAO.selectForUpdate(loginUser.getOrgId(), id);
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
