package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowFormDAO;
import com.plasticene.boot.flow.core.entity.FlowForm;
import com.plasticene.boot.flow.core.model.param.FlowFormParam;
import com.plasticene.boot.flow.core.model.query.FlowFormQuery;
import com.plasticene.boot.flow.core.model.vo.FlowFormVO;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.flow.core.service.FlowFormService;
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
    private FlowCategoryService flowCategoryService;


    /**
     * 校验动态表单配置，在操作者所属组织内创建表单。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 新建记录 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FlowFormParam param, LoginUser loginUser) {
        FlowFormConfig.validate(param.getFormConfig());
        FlowForm form = PtcBeanUtils.copy(param, FlowForm.class);
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

    /**
     * 分页查询操作者所属组织的表单并补全分类名称。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowFormVO> page(FlowFormQuery query, LoginUser loginUser) {
        PtcLambdaQueryWrapper<FlowForm> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowForm::getOrgId, loginUser.getOrgId());
        queryWrapper.likeIfPresent(FlowForm::getName, query.getName());
        queryWrapper.eqIfPresent(FlowForm::getCategory, query.getCategory());
        queryWrapper.orderByDesc(FlowForm::getId);
        PageResult<FlowForm> result = flowFormDAO.selectPage(query, queryWrapper);
        List<FlowFormVO> voList = PtcBeanUtils.copyList(result.getList(), FlowFormVO.class);
        fillCategoryNames(voList, loginUser);
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    /**
     * 查询操作者所属组织的全部表单并补全分类名称。
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    @Override
    public List<FlowFormVO> listAll(LoginUser loginUser) {
        PtcLambdaQueryWrapper<FlowForm> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowForm::getOrgId, loginUser.getOrgId());
        queryWrapper.orderByDesc(FlowForm::getId);
        List<FlowForm> list = this.list(queryWrapper);
        List<FlowFormVO> voList = PtcBeanUtils.copyList(list, FlowFormVO.class);
        fillCategoryNames(voList, loginUser);
        return voList;
    }

    /**
     * 查询表单详情并使用操作者所属组织的分类映射补全名称。
     * @param id 记录 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    @Override
    public FlowFormVO detail(Long id, LoginUser loginUser) {
        FlowForm form = this.getById(id);
        FlowFormVO vo = PtcBeanUtils.copy(form, FlowFormVO.class);
        if (vo != null) {
            fillCategoryNames(List.of(vo), loginUser);
        }
        return vo;
    }
    /**
     * 使用操作者所属组织的分类映射补全表单分类名称。
     * @param forms 待补全分类名称的表单
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    private void fillCategoryNames(List<FlowFormVO> forms, LoginUser loginUser) {
        if (CollUtil.isEmpty(forms)) {
            return;
        }
        Map<String, String> categories = flowCategoryService.getCategoryMap(loginUser);
        forms.forEach(form -> form.setCategoryName(form.getCategory() == null ? "" :
                categories.getOrDefault(form.getCategory(), form.getCategory())));
    }
}
