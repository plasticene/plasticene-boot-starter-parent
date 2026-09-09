
package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.enums.FlowModelEnums;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.model.vo.FlowModelStatisticsVO;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowModelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.model.query.FlowModelQuery;
import com.plasticene.boot.flow.core.model.vo.FlowModelVO;
import cn.hutool.core.collection.CollUtil;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.validator.FlowNodeValidator;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.plasticene.boot.common.pojo.PageResult;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * <p> 工作流-流程模型表 </p>
 *
 * @author ZFJ
 * @since 2026-04-13
 */

@Service
public class FlowModelServiceImpl extends ServiceImpl<FlowModelDAO, FlowModel> implements FlowModelService {
    @Resource
    private FlowModelDAO flowModelDAO;
    @Resource
    private CategoryService categoryService;
    @Resource
    private FlowNodeValidator flowNodeValidator;
    @Resource
    private FlowDefinitionService flowDefinitionService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long create(FlowModelParam param) {
        FlowModel flowModel = PtcBeanUtils.copy(param, FlowModel.class);
        flowModel.setOrgId(LoginUserHolder.get().getOrgId());
        flowModel.setIsEdited(CommonConstant.IS_ON);
        flowModelDAO.insert(flowModel);
        return flowModel.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(FlowModelParam param) {
        FlowModel flowModel = PtcBeanUtils.copy(param, FlowModel.class);
        flowModel.setIsEdited(CommonConstant.IS_ON);
        flowModelDAO.updateById(flowModel);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Long> idList) {
        if (CollUtil.isEmpty(idList)) {
            return;
        }
        flowModelDAO.deleteByIds(idList);
    }

    @Override
    public PageResult<FlowModelVO> page(FlowModelQuery query) {
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = buildQueryWrapper(query);
        queryWrapper.select(FlowModel::getId, FlowModel::getName, FlowModel::getCode, FlowModel::getCategory,
                FlowModel::getStatus, FlowModel::getStartUserType, FlowModel::getStartUserIds,
                FlowModel::getStartRoleIds, FlowModel::getStartDeptIds, FlowModel::getManagerUserIds,
                FlowModel::getRemark, FlowModel::getActiveDefinitionId, FlowModel::getActiveVersion,
                FlowModel::getPublishTime, FlowModel::getUpdateTime, FlowModel::getIsEdited);
        queryWrapper.orderByDesc(FlowModel::getId);
        PageResult<FlowModel> result = flowModelDAO.selectPage(query, queryWrapper);
        List<FlowModelVO> voList = PtcBeanUtils.copyList(result.getList(), FlowModelVO.class);
        Map<String, String> categoryMap = categoryService.getCategoryMap();
        voList.forEach(vo -> vo.setCategoryName(categoryMap.get(vo.getCategory())));
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    @Override
    public FlowModelVO detail(Long id) {
        FlowModel flowModel = flowModelDAO.selectById(id);
        return PtcBeanUtils.copy(flowModel, FlowModelVO.class);
    }

    @Override
    public FlowModelStatisticsVO statistics(FlowModelQuery query) {
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = buildQueryWrapper(query);
        queryWrapper.select(FlowModel::getId, FlowModel::getStatus);
        List<FlowModel> modelList = flowModelDAO.selectList(queryWrapper);
        FlowModelStatisticsVO vo = new FlowModelStatisticsVO();
        vo.setTotal((long) modelList.size());
        vo.setPublished(modelList.stream()
                .filter(model -> Objects.equals(model.getStatus(),FlowModelEnums.Status.PUBLISHED.getCode())
                || Objects.equals(model.getStatus(),FlowModelEnums.Status.DISABLED.getCode()))
                .count());
        vo.setStop(modelList.stream()
                .filter(model -> Objects.equals(model.getStatus(), FlowModelEnums.Status.DISABLED.getCode()))
                .count());
        vo.setRunning(vo.getPublished() - vo.getStop());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void enable(FlowModelEnableParam param) {
        Long id = param.getId();
        Integer status = param.getStatus();
        FlowModel flowModel = this.getById(id);
        Assert.notNull(flowModel, "流程模型不存在");
        if (Objects.equals(flowModel.getStatus(), FlowModelEnums.Status.DRAFT.getCode())) {
            throw new BizException("流程模型当前状态为草稿，不能进行开关操作");
        }
        LambdaUpdateWrapper<FlowModel> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(FlowModel::getStatus, status);
        updateWrapper.eq(FlowModel::getId, id);
        // 关闭→开启
        if (Objects.equals(status, FlowModelEnums.Status.PUBLISHED.getCode())) {
            updateWrapper.eq(FlowModel::getStatus, FlowModelEnums.Status.DISABLED.getCode());
        }
        // 开启→关闭
        if (Objects.equals(status, FlowModelEnums.Status.DISABLED.getCode())) {
            updateWrapper.eq(FlowModel::getStatus, FlowModelEnums.Status.PUBLISHED.getCode());
        }
        flowModelDAO.update(updateWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void publish(Long id) {
        // 1.获取流程模型,悲观锁防止并发发布
        FlowModel flowModel = flowModelDAO.selectFlowModelForUpdate(id);
        Assert.notNull(flowModel, "流程模型不存在");
        // 2.发布前校验流程模型是否合法
        FlowNode modelNode = flowModel.getModelNode();
        flowNodeValidator.validate(modelNode);
        // 3.生成发布流程定义快照
        FlowDefinition flowDefinition = PtcBeanUtils.copy(flowModel, FlowDefinition.class);
        flowDefinition.setId(null);
        flowDefinition.setModelId(id);
        int maxVersion = flowDefinitionService.getMaxVersion(id);
        flowDefinition.setVersion(maxVersion + 1);
        flowDefinitionService.save(flowDefinition);

        // 4.更新model模型状态
        FlowModel updateModel = new FlowModel();
        updateModel.setId(id);
        updateModel.setStatus(FlowModelEnums.Status.PUBLISHED.getCode());
        updateModel.setActiveModel(modelNode);
        updateModel.setActiveDefinitionId(flowDefinition.getId());
        updateModel.setActiveVersion(flowDefinition.getVersion());
        updateModel.setIsEdited(CommonConstant.IS_OFF);
        updateModel.setPublishTime(LocalDateTime.now());
        flowModelDAO.updateById(updateModel);
    }

    PtcLambdaQueryWrapper<FlowModel> buildQueryWrapper(FlowModelQuery query) {
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowModel::getOrgId, LoginUserHolder.get().getOrgId())
                .eqIfPresent(FlowModel::getCategory, query.getCategory())
                .eqIfPresent(FlowModel::getStatus, query.getStatus())
                .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> {
                    wrapper.like(FlowModel::getName, query.getKeyword())
                            .or()
                            .like(FlowModel::getCode, query.getKeyword());
                });
        return queryWrapper;
    }
}

