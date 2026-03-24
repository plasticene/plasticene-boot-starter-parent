package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FormDAO;
import com.plasticene.boot.flow.core.entity.Form;
import com.plasticene.boot.flow.core.model.param.FormParam;
import com.plasticene.boot.flow.core.model.query.FormQuery;
import com.plasticene.boot.flow.core.model.vo.FormVO;
import com.plasticene.boot.flow.core.service.FormService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author ZFJ
 * @since 2026/2/25
 */
@Service
public class FormServiceImpl extends ServiceImpl<FormDAO, Form> implements FormService {
    @Resource
    private FormDAO formDAO;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FormParam param) {
        Form form = PtcBeanUtils.copy(param, Form.class);
        LoginUser loginUser = LoginUserHolder.get();
        form.setOrgId(loginUser.getOrgId());
        formDAO.insert(form);
        return form.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FormParam param) {
        Form form = PtcBeanUtils.copy(param, Form.class);
        formDAO.updateById(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        formDAO.deleteByIds(ids);
    }

    @Override
    public PageResult<FormVO> page(FormQuery query) {
        PtcLambdaQueryWrapper<Form> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(Form::getOrgId, LoginUserHolder.get().getOrgId());
        queryWrapper.likeIfPresent(Form::getName, query.getName());
        queryWrapper.orderByDesc(Form::getId);
        PageResult<Form> result = formDAO.selectPage(query, queryWrapper);
        List<FormVO> voList = PtcBeanUtils.copyList(result.getList(), FormVO.class);
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    @Override
    public List<FormVO> listAll() {
        PtcLambdaQueryWrapper<Form> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(Form::getOrgId, LoginUserHolder.get().getOrgId());
        queryWrapper.orderByDesc(Form::getId);
        List<Form> list = this.list(queryWrapper);
        return PtcBeanUtils.copyList(list, FormVO.class);
    }

    @Override
    public FormVO detail(Long id) {
        Form form = this.getById(id);
        return PtcBeanUtils.copy(form, FormVO.class);
    }
}
