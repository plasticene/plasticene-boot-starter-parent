package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowFormDAO;
import com.plasticene.boot.flow.core.entity.FlowForm;
import com.plasticene.boot.flow.core.model.param.FlowFormParam;
import com.plasticene.boot.flow.core.model.query.FlowFormQuery;
import com.plasticene.boot.flow.core.model.vo.FlowFormVO;
import com.plasticene.boot.flow.core.service.FlowFormService;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.utils.FlowFormConfig;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 实现流程表单保存、查询与分类回显。
 * @author ZFJ
 * @since 2026-11-05
 */
@Service
public class FlowFormServiceImpl extends ServiceImpl<FlowFormDAO, FlowForm> implements FlowFormService {
    @Resource
    private FlowFormDAO flowFormDAO;
    @Resource
    private CategoryService categoryService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FlowFormParam param) {
        FlowFormConfig.validate(param.getFormConfig());
        FlowForm form = PtcBeanUtils.copy(param, FlowForm.class);
        LoginUser loginUser = LoginUserHolder.get();
        form.setOrgId(loginUser.getOrgId());
        flowFormDAO.insert(form);
        return form.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FlowFormParam param) {
        FlowFormConfig.validate(param.getFormConfig());
        FlowForm form = PtcBeanUtils.copy(param, FlowForm.class);
        flowFormDAO.updateById(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        flowFormDAO.deleteByIds(ids);
    }

    @Override
    public PageResult<FlowFormVO> page(FlowFormQuery query) {
        PtcLambdaQueryWrapper<FlowForm> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowForm::getOrgId, LoginUserHolder.get().getOrgId());
        queryWrapper.likeIfPresent(FlowForm::getName, query.getName());
        queryWrapper.eqIfPresent(FlowForm::getCategory, query.getCategory());
        queryWrapper.orderByDesc(FlowForm::getId);
        PageResult<FlowForm> result = flowFormDAO.selectPage(query, queryWrapper);
        List<FlowFormVO> voList = PtcBeanUtils.copyList(result.getList(), FlowFormVO.class);
        fillCategoryNames(voList);
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    @Override
    public List<FlowFormVO> listAll() {
        PtcLambdaQueryWrapper<FlowForm> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowForm::getOrgId, LoginUserHolder.get().getOrgId());
        queryWrapper.orderByDesc(FlowForm::getId);
        List<FlowForm> list = this.list(queryWrapper);
        List<FlowFormVO> voList = PtcBeanUtils.copyList(list, FlowFormVO.class);
        fillCategoryNames(voList);
        return voList;
    }

    @Override
    public FlowFormVO detail(Long id) {
        FlowForm form = this.getById(id);
        FlowFormVO vo = PtcBeanUtils.copy(form, FlowFormVO.class);
        if (vo != null) {
            fillCategoryNames(List.of(vo));
        }
        return vo;
    }
    private void fillCategoryNames(List<FlowFormVO> forms) {
        if (CollUtil.isEmpty(forms)) {
            return;
        }
        Map<String, String> categories = categoryService.getCategoryMap();
        forms.forEach(form -> form.setCategoryName(form.getCategory() == null ? "" :
                categories.getOrDefault(form.getCategory(), form.getCategory())));
    }
}
