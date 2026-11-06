
package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.enums.FlowModelEnums;
import com.plasticene.boot.flow.core.model.dto.FlowExtraSetting;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.param.FlowModelEnableParam;
import com.plasticene.boot.flow.core.model.param.FlowModelParam;
import com.plasticene.boot.flow.core.model.query.FlowModelQuery;
import com.plasticene.boot.flow.core.model.vo.FlowModelStatisticsVO;
import com.plasticene.boot.flow.core.model.vo.FlowModelVO;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowModelService;
import com.plasticene.boot.flow.core.utils.FlowFormConfig;
import com.plasticene.boot.flow.core.validator.FlowNodeValidator;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private FlowCategoryService flowCategoryService;
    @Resource
    private FlowNodeValidator flowNodeValidator;
    @Resource
    private FlowDefinitionService flowDefinitionService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long create(FlowModelParam param) {
        FlowFormConfig.validate(param.getFormConfig());
        FlowModel flowModel = PtcBeanUtils.copy(param, FlowModel.class);
        flowModel.setExtraSetting(FlowExtraSetting.normalize(flowModel.getExtraSetting()));
        if (flowModel.getExtraSetting().getTimeoutRule() != null) {
            flowModel.getExtraSetting().getTimeoutRule().validate();
        }
        flowModel.setOrgId(LoginUserHolder.get().getOrgId());
        flowModel.setIsEdited(CommonConstant.IS_ON);
        validateCodeUnique(flowModel.getCode(), null);
        try {
            flowModelDAO.insert(flowModel);
        } catch (DuplicateKeyException exception) {
            throw new BizException("流程标识已存在");
        }
        return flowModel.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(FlowModelParam param) {
        FlowFormConfig.validate(param.getFormConfig());
        FlowModel flowModel = PtcBeanUtils.copy(param, FlowModel.class);
        if (flowModel.getExtraSetting() != null) {
            flowModel.setExtraSetting(FlowExtraSetting.normalize(flowModel.getExtraSetting()));
            if (flowModel.getExtraSetting().getTimeoutRule() != null) {
                flowModel.getExtraSetting().getTimeoutRule().validate();
            }
        }
        flowModel.setIsEdited(CommonConstant.IS_ON);
        validateCodeUnique(flowModel.getCode(), flowModel.getId());
        try {
            flowModelDAO.updateById(flowModel);
        } catch (DuplicateKeyException exception) {
            throw new BizException("流程标识已存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Long> idList) {
        if (CollUtil.isEmpty(idList)) {
            return;
        }
        Long orgId = LoginUserHolder.get().getOrgId();
        List<Long> modelIds = new java.util.ArrayList<>();
        // 与发布操作使用相同的行锁，防止删除期间新增未标记的发布版本。
        for (Long modelId : idList.stream().distinct().sorted().toList()) {
            FlowModel model = flowModelDAO.selectFlowModelForUpdate(modelId);
            if (model == null) {
                continue;
            }
            if (!Objects.equals(model.getOrgId(), orgId)) {
                throw new BizException("流程模型不存在");
            }
            modelIds.add(modelId);
        }
        if (modelIds.isEmpty()) {
            return;
        }
        // 不使用 @TableLogic，已有实例仍需按版本 ID 读取快照。
        LambdaUpdateWrapper<FlowDefinition> versions = new LambdaUpdateWrapper<>();
        versions.in(FlowDefinition::getModelId, modelIds)
                .eq(FlowDefinition::getOrgId, orgId)
                .set(FlowDefinition::getIsDelete, CommonConstant.IS_DEL);
        flowDefinitionService.update(versions);
        flowModelDAO.deleteByIds(modelIds);
    }

    @Override
    public PageResult<FlowModelVO> page(FlowModelQuery query) {
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = buildQueryWrapper(query);
        queryWrapper.select(FlowModel::getId, FlowModel::getName, FlowModel::getIcon, FlowModel::getCode, FlowModel::getCategory,
                FlowModel::getStatus, FlowModel::getStartUserType, FlowModel::getStartUserIds,
                FlowModel::getStartRoleIds, FlowModel::getStartDeptIds, FlowModel::getManagerUserIds,
                FlowModel::getRemark, FlowModel::getActiveDefinitionId, FlowModel::getActiveVersion,
                FlowModel::getPublishTime, FlowModel::getUpdateTime, FlowModel::getIsEdited);
        queryWrapper.orderByDesc(FlowModel::getId);
        PageResult<FlowModel> result = flowModelDAO.selectPage(query, queryWrapper);
        List<FlowModelVO> voList = PtcBeanUtils.copyList(result.getList(), FlowModelVO.class);
        Map<String, String> categoryMap = flowCategoryService.getCategoryMap();
        voList.forEach(vo -> vo.setCategoryName(categoryMap.get(vo.getCategory())));
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    @Override
    public FlowModelVO detail(Long id) {
        FlowModel flowModel = flowModelDAO.selectById(id);
        FlowModelVO vo = PtcBeanUtils.copy(flowModel, FlowModelVO.class);
        vo.setExtraSetting(FlowExtraSetting.normalize(vo.getExtraSetting()));
        return vo;
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
        flowNodeValidator.validate(modelNode, FlowFormConfig.referenceFields(flowModel.getFormConfig()));
        // 3.生成发布流程定义快照
        FlowDefinition flowDefinition = PtcBeanUtils.copy(flowModel, FlowDefinition.class);
        flowDefinition.setExtraSetting(FlowExtraSetting.normalize(flowDefinition.getExtraSetting()));
        flowDefinition.setId(null);
        flowDefinition.setModelId(id);
        flowDefinition.setIsDelete(CommonConstant.IS_NOT_DEL);
        // 由插入时的审计填充记录本次发布人，不能沿用模型最初创建者。
        flowDefinition.setCreator(null);
        flowDefinition.setUpdater(null);
        flowDefinition.setCreateTime(LocalDateTime.now());
        flowDefinition.setUpdateTime(LocalDateTime.now());
        flowDefinition.setPublishTime(LocalDateTime.now());
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
                .and(StrUtil.isNotBlank(query.getKeyword()),
                        wrapper -> wrapper.like(FlowModel::getName, query.getKeyword())
                        .or()
                        .like(FlowModel::getCode, query.getKeyword()));
        return queryWrapper;
    }

    private void validateCodeUnique(String code, Long excludeId) {
        // 局部更新未传标识时保持原值。
        if (code == null) {
            return;
        }
        if (code.length() < 5) {
            FlowModel existing = excludeId == null ? null : flowModelDAO.selectById(excludeId);
            if (code.isEmpty() || existing == null
                    || !Objects.equals(existing.getOrgId(), LoginUserHolder.get().getOrgId())
                    || !Objects.equals(existing.getCode(), code)) {
                throw new BizException("流程标识至少5位");
            }
        }
        if (flowModelDAO.countByCode(LoginUserHolder.get().getOrgId(), code, excludeId) > 0) {
            throw new BizException("流程标识已存在");
        }
    }
}

