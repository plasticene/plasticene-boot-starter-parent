package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowCategoryDAO;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private FlowCategoryDAO flowCategoryDAO;
    @Resource
    private FlowCategoryService flowCategoryService;
    @Resource
    private FlowNodeValidator flowNodeValidator;
    @Resource
    private FlowDefinitionService flowDefinitionService;

    /**
     * 在操作者所属组织创建草稿模型，校验表单、分类和流程编码。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 新建记录 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long create(FlowModelParam param, LoginUser loginUser) {
        FlowFormConfig.validate(param.getFormConfig());
        FlowModel flowModel = PtcBeanUtils.copy(param, FlowModel.class);
        flowModel.setExtraSetting(FlowExtraSetting.normalize(flowModel.getExtraSetting()));
        if (flowModel.getExtraSetting().getTimeoutRule() != null) {
            flowModel.getExtraSetting().getTimeoutRule().validate();
        }
        flowModel.setStatus(FlowModelEnums.Status.DRAFT.getCode());
        flowModel.setOrgId(loginUser.getOrgId());
        flowModel.setIsEdited(CommonConstant.IS_ON);
        validateCodeUnique(flowModel.getCode(), null, loginUser);
        try {
            requireCategory(flowModel.getCategory(), loginUser);
            flowModelDAO.insert(flowModel);
        } catch (DuplicateKeyException exception) {
            throw new BizException("流程标识已存在");
        }
        return flowModel.getId();
    }

    /**
     * 更新操作者所属组织的流程模型，并校验流程管理员权限。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(FlowModelParam param, LoginUser loginUser) {
        FlowFormConfig.validate(param.getFormConfig());
        FlowModel flowModel = PtcBeanUtils.copy(param, FlowModel.class);
        if (flowModel.getExtraSetting() != null) {
            flowModel.setExtraSetting(FlowExtraSetting.normalize(flowModel.getExtraSetting()));
            if (flowModel.getExtraSetting().getTimeoutRule() != null) {
                flowModel.getExtraSetting().getTimeoutRule().validate();
            }
        }
        requireManager(requireModel(flowModelDAO.selectFlowModelForUpdate(param.getId()), loginUser), loginUser);
        flowModel.setIsEdited(CommonConstant.IS_ON);
        validateCodeUnique(flowModel.getCode(), flowModel.getId(), loginUser);
        try {
            if (flowModel.getCategory() != null) {
                requireCategory(flowModel.getCategory(), loginUser);
            }
            flowModelDAO.updateById(flowModel);
        } catch (DuplicateKeyException exception) {
            throw new BizException("流程标识已存在");
        }
    }

    /**
     * 校验组织及流程管理员权限，删除模型并标记其发布版本，不影响已有实例快照。
     * @param idList 待操作的模型 ID 列表
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Long> idList, LoginUser loginUser) {
        if (CollUtil.isEmpty(idList)) {
            return;
        }
        Long orgId = loginUser.getOrgId();
        List<Long> modelIds = new ArrayList<>();
        // 与发布操作使用相同的行锁，防止删除期间新增未标记的发布版本。
        for (Long modelId : idList.stream().distinct().sorted().toList()) {
            FlowModel model = flowModelDAO.selectFlowModelForUpdate(modelId);
            if (model == null) {
                continue;
            }
            requireManager(requireModel(model, loginUser), loginUser);
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

    /**
     * 分页查询操作者所属组织的流程模型并补全分类名称。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 分页结果
     */
    @Override
    public PageResult<FlowModelVO> page(FlowModelQuery query, LoginUser loginUser) {
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = buildQueryWrapper(query, loginUser);
        queryWrapper.select(FlowModel::getId, FlowModel::getName, FlowModel::getIcon, FlowModel::getCode, FlowModel::getCategory,
                FlowModel::getStatus, FlowModel::getStartUserType, FlowModel::getStartUserIds,
                FlowModel::getStartRoleIds, FlowModel::getStartDeptIds, FlowModel::getManagerUserIds,
                FlowModel::getRemark, FlowModel::getActiveDefinitionId, FlowModel::getActiveVersion,
                FlowModel::getPublishTime, FlowModel::getUpdateTime, FlowModel::getIsEdited);
        queryWrapper.orderByDesc(FlowModel::getId);
        PageResult<FlowModel> result = flowModelDAO.selectPage(query, queryWrapper);
        List<FlowModelVO> voList = PtcBeanUtils.copyList(result.getList(), FlowModelVO.class);
        Map<String, String> categoryMap = flowCategoryService.getCategoryMap(loginUser);
        voList.forEach(vo -> vo.setCategoryName(categoryMap.get(vo.getCategory())));
        return new PageResult<>(voList, result.getTotal(), result.getPages());
    }

    /**
     * 校验模型组织归属并返回模型详情。
     * @param id 记录 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    @Override
    public FlowModelVO detail(Long id, LoginUser loginUser) {
        FlowModel flowModel = requireModel(flowModelDAO.selectById(id), loginUser);
        FlowModelVO vo = PtcBeanUtils.copy(flowModel, FlowModelVO.class);
        vo.setExtraSetting(FlowExtraSetting.normalize(vo.getExtraSetting()));
        return vo;
    }

    /**
     * 统计操作者所属组织中符合查询条件的流程模型数量。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    @Override
    public FlowModelStatisticsVO statistics(FlowModelQuery query, LoginUser loginUser) {
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = buildQueryWrapper(query, loginUser);
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

    /**
     * 校验操作者的流程管理员权限，启用或停用已发布的流程模型。
     * @param param 操作参数
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void enable(FlowModelEnableParam param, LoginUser loginUser) {
        Long id = param.getId();
        Integer status = param.getStatus();
        FlowModel flowModel = requireModel(flowModelDAO.selectById(id), loginUser);
        requireManager(flowModel, loginUser);
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

    /**
     * 校验组织和流程管理员权限，锁定模型并生成新的发布快照。
     * @param id 记录 ID
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void publish(Long id, LoginUser loginUser) {
        // 1.获取流程模型,悲观锁防止并发发布
        FlowModel flowModel = flowModelDAO.selectFlowModelForUpdate(id);
        requireManager(requireModel(flowModel, loginUser), loginUser);
        // 2.发布前校验流程模型是否合法
        FlowNode modelNode = flowModel.getModelNode();
        requireCategory(flowModel.getCategory(), loginUser);
        flowNodeValidator.validate(modelNode, flowModel.getFormConfig() == null
                ? null : FlowFormConfig.referenceFields(flowModel.getFormConfig()));
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

    /**
     * 根据操作者所属组织和筛选条件构建流程模型查询。
     * @param query 查询条件
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    PtcLambdaQueryWrapper<FlowModel> buildQueryWrapper(FlowModelQuery query, LoginUser loginUser) {
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowModel::getOrgId, loginUser.getOrgId())
                .eqIfPresent(FlowModel::getCategory, query.getCategory())
                .eqIfPresent(FlowModel::getStatus, query.getStatus())
                .and(StrUtil.isNotBlank(query.getKeyword()),
                        wrapper -> wrapper.like(FlowModel::getName, query.getKeyword())
                        .or()
                        .like(FlowModel::getCode, query.getKeyword()));
        return queryWrapper;
    }

    /**
     * 校验模型存在且归属于操作者所属组织。
     * @param model 流程模型
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     * @return 查询结果
     */
    private FlowModel requireModel(FlowModel model, LoginUser loginUser) {
        if (model == null || !Objects.equals(model.getOrgId(), loginUser.getOrgId())) {
            throw new BizException("流程模型不存在");
        }
        return model;
    }

    /**
     * 锁定分类并校验其存在且归属于操作者所属组织。
     * @param category 流程分类编码
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    private void requireCategory(String category, LoginUser loginUser) {
        if (category == null || flowCategoryDAO.selectByCodeForUpdate(
                loginUser.getOrgId(), category) == null) {
            throw new BizException("流程分类不存在或不属于当前组织");
        }
    }

    /**
     * 校验操作者属于模型配置的流程管理员。
     * @param model 流程模型
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    private void requireManager(FlowModel model, LoginUser loginUser) {
        Long userId = loginUser.getId();
        if (userId == null || !CollUtil.contains(model.getManagerUserIds(), userId)) {
            throw new BizException("仅流程管理员可操作该流程模型");
        }
    }

    /**
     * 校验模型编码的组织内唯一性、最小长度及发布后不可变规则。
     * @param code 流程模型编码
     * @param excludeId 更新时排除的模型 ID，创建时为空
     * @param loginUser 宿主提供的可信操作者及其组织上下文
     */
    private void validateCodeUnique(String code, Long excludeId, LoginUser loginUser) {
        // 局部更新未传标识时保持原值。
        if (excludeId != null && code != null) {
            FlowModel published = flowModelDAO.selectById(excludeId);
            if (published != null && flowDefinitionService.getMaxVersion(excludeId) > 0
                    && !Objects.equals(published.getCode(), code)) {
                throw new BizException("流程标识发布后不可修改");
            }
        }
        if (code == null) {
            return;
        }
        if (code.length() < 5) {
            FlowModel existing = excludeId == null ? null : flowModelDAO.selectById(excludeId);
            if (code.isEmpty() || existing == null
                    || !Objects.equals(existing.getOrgId(), loginUser.getOrgId())
                    || !Objects.equals(existing.getCode(), code)) {
                throw new BizException("流程标识至少5位");
            }
        }
        if (flowModelDAO.countByCode(loginUser.getOrgId(), code, excludeId) > 0) {
            throw new BizException("流程标识已存在");
        }
    }
}
