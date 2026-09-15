package com.plasticene.boot.flow.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowDefinitionDAO;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.dao.FlowModelDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowModel;
import com.plasticene.boot.flow.core.enums.FlowModelEnums;
import com.plasticene.boot.flow.core.model.vo.FlowDefinitionVO;
import com.plasticene.boot.flow.core.model.vo.FlowStartCatalogVO;
import com.plasticene.boot.flow.core.model.vo.FlowStartableModelVO;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ZFJ
 * @since 2026/4/15
 */
@Service
public class FlowDefinitionServiceImpl extends ServiceImpl<FlowDefinitionDAO, FlowDefinition> implements FlowDefinitionService {
    @Resource
    private FlowDefinitionDAO flowDefinitionDAO;
    @Resource
    private FlowModelDAO flowModelDAO;
    @Resource
    private CategoryService categoryService;
    @Resource
    private FlowInstanceDAO flowInstanceDAO;


    @Override
    public int getMaxVersion(Long modelId) {
        Integer maxVersion = flowDefinitionDAO.getMaxVersion(modelId);
        return maxVersion == null ? 0 : maxVersion;
    }

    @Override
    public FlowStartCatalogVO getStartableCatalog() {
        LoginUser loginUser = LoginUserHolder.get();
        // 查询已发布的流程模型flowModel
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowModel::getOrgId, loginUser.getOrgId())
                .eq(FlowModel::getStatus, FlowModelEnums.Status.PUBLISHED.getCode())
                .isNotNull(FlowModel::getActiveDefinitionId)
                .orderByDesc(FlowModel::getPublishTime);
        List<FlowModel> publishedModels = flowModelDAO.selectList(queryWrapper);
        // 根据发布id查询flowDefinition
        Map<Long, FlowDefinition> definitionMap = publishedModels.isEmpty()
                ? Map.of()
                : this.listByIds(publishedModels.stream()
                        .map(FlowModel::getActiveDefinitionId)
                        .toList()).stream()
                .collect(Collectors.toMap(FlowDefinition::getId, Function.identity()));
        // 过滤出当前用户可发起的流程模型
        List<FlowModel> models = publishedModels.stream()
                .filter(model -> {
                    FlowDefinition definition = definitionMap.get(model.getActiveDefinitionId());
                    return definition != null
                            && Objects.equals(definition.getModelId(), model.getId())
                            && Objects.equals(definition.getOrgId(), loginUser.getOrgId())
                            && canStart(definition, loginUser);
                })
                .toList();

        // 构造可发起的流程模型分类
        Map<String, String> categoryMap = categoryService.getCategoryMap();
        List<FlowStartableModelVO> modelVOList = models.stream()
                .map(model -> toStartableModelVO(model, categoryMap))
                .toList();
        Map<Long, FlowStartableModelVO> modelVOMap = modelVOList.stream()
                .collect(Collectors.toMap(FlowStartableModelVO::getModelId, Function.identity()));

        Set<Long> recentModelIds = findRecentModelIds(loginUser, modelVOMap);
        FlowStartCatalogVO catalogVO = new FlowStartCatalogVO();
        catalogVO.setModels(modelVOList);
        catalogVO.setRecentModelIds(List.copyOf(recentModelIds));
        return catalogVO;
    }

    @Override
    public FlowDefinitionVO getFlowDefinition(Long definitionId) {
        FlowDefinition flowDefinition = getStartableDefinition(definitionId);
        Map<String, String> categoryMap = categoryService.getCategoryMap();
        FlowDefinitionVO vo = PtcBeanUtils.copy(flowDefinition, FlowDefinitionVO.class);
        vo.setDefinitionId(flowDefinition.getId());
        vo.setCategoryName(categoryMap.get(flowDefinition.getCategory()));
        return vo;
    }

    @Override
    public FlowDefinition getStartableDefinition(Long definitionId) {
        if (definitionId == null) {
            throw new BizException("流程发布id不能为空");
        }
        LoginUser loginUser = LoginUserHolder.get();
        // 校验流程模型发布信息
        PtcLambdaQueryWrapper<FlowModel> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowModel::getOrgId, loginUser.getOrgId())
                .eq(FlowModel::getStatus, FlowModelEnums.Status.PUBLISHED.getCode())
                .eq(FlowModel::getActiveDefinitionId, definitionId);
        FlowModel model = flowModelDAO.selectOne(queryWrapper);
        if (model == null) {
            throw new BizException("流程已停用或发布版本已变更，请刷新后重试");
        }
        FlowDefinition definition = this.getById(definitionId);
        if (definition == null
                || !Objects.equals(definition.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(definition.getModelId(), model.getId())) {
            throw new BizException("流程发布定义不存在");
        }
        if (!canStart(definition, loginUser)) {
            throw new BizException("当前用户无权发起该流程");
        }
        return definition;
    }

    private Set<Long> findRecentModelIds(LoginUser loginUser, Map<Long, FlowStartableModelVO> modelVOMap) {
        if (modelVOMap.isEmpty()) {
            return Set.of();
        }
        PtcLambdaQueryWrapper<FlowInstance> instanceQuery = new PtcLambdaQueryWrapper<>();
        instanceQuery.eq(FlowInstance::getOrgId, loginUser.getOrgId())
                .eq(FlowInstance::getUserId, loginUser.getId())
                .select(FlowInstance::getDefinitionId, FlowInstance::getStartTime)
                .orderByDesc(FlowInstance::getStartTime)
                .last("LIMIT 100");
        List<FlowInstance> instances = flowInstanceDAO.selectList(instanceQuery);
        if (instances.isEmpty()) {
            return Set.of();
        }

        List<Long> definitionIds = instances.stream()
                .map(FlowInstance::getDefinitionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (definitionIds.isEmpty()) {
            return Set.of();
        }
        Map<Long, Long> definitionModelMap = this.listByIds(definitionIds).stream()
                .collect(Collectors.toMap(FlowDefinition::getId, FlowDefinition::getModelId));
        Set<Long> recentModelIds = new LinkedHashSet<>();
        Map<Long, LocalDateTime> lastStartTimeMap = new HashMap<>();
        for (FlowInstance instance : instances) {
            Long modelId = definitionModelMap.get(instance.getDefinitionId());
            if (modelId == null || !modelVOMap.containsKey(modelId)) {
                continue;
            }
            lastStartTimeMap.putIfAbsent(modelId, instance.getStartTime());
            recentModelIds.add(modelId);
            if (recentModelIds.size() >= 3) {
                break;
            }
        }
        lastStartTimeMap.forEach((modelId, startTime) -> modelVOMap.get(modelId).setLastStartTime(startTime));
        return recentModelIds;
    }

    private FlowStartableModelVO toStartableModelVO(FlowModel model, Map<String, String> categoryMap) {
        FlowStartableModelVO vo = new FlowStartableModelVO();
        vo.setModelId(model.getId());
        vo.setDefinitionId(model.getActiveDefinitionId());
        vo.setCode(model.getCode());
        vo.setName(model.getName());
        vo.setCategory(model.getCategory());
        vo.setCategoryName(categoryMap.get(model.getCategory()));
        vo.setRemark(model.getRemark());
        vo.setVersion(model.getActiveVersion());
        vo.setPublishTime(model.getPublishTime());
        return vo;
    }



    boolean canStart(FlowDefinition definition, LoginUser loginUser) {
        Integer startUserType = definition.getStartUserType();
        if (Objects.equals(startUserType, FlowModelEnums.StartUserType.ALL.getCode())) {
            return true;
        }
        if (Objects.equals(startUserType, FlowModelEnums.StartUserType.USER.getCode())) {
            return contains(definition.getStartUserIds(), loginUser.getId());
        }
        if (Objects.equals(startUserType, FlowModelEnums.StartUserType.DEPT.getCode())) {
            return contains(definition.getStartDeptIds(), loginUser.getDeptId());
        }
        if (Objects.equals(startUserType, FlowModelEnums.StartUserType.ROLE.getCode())) {
            return intersects(definition.getStartRoleIds(), loginUser.getRoleIds());
        }
        return false;
    }

    private boolean contains(List<Long> values, Long value) {
        return value != null && values != null && values.contains(value);
    }

    private boolean intersects(List<Long> left, List<Long> right) {
        return left != null && right != null && left.stream().anyMatch(right::contains);
    }
}
