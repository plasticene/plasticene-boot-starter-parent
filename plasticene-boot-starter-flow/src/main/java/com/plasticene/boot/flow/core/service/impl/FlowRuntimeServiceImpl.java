package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.LoginUserHolder;
import com.plasticene.boot.flow.core.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.entity.FlowDefinition;
import com.plasticene.boot.flow.core.entity.FlowInstance;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.enums.FlowInstanceEventTypeEnum;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.event.InstanceEvent;
import com.plasticene.boot.flow.core.executor.ProcessExecutor;
import com.plasticene.boot.flow.core.model.dto.FlowInstanceNodeTimeDTO;
import com.plasticene.boot.flow.core.model.dto.FlowNode;
import com.plasticene.boot.flow.core.model.query.FlowInstanceQuery;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceDetailVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceStatisticsVO;
import com.plasticene.boot.flow.core.model.vo.FlowInstanceVO;
import com.plasticene.boot.flow.core.model.vo.FlowRouteNodeVO;
import com.plasticene.boot.flow.core.model.vo.FlowTaskVO;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.service.FlowDefinitionService;
import com.plasticene.boot.flow.core.service.FlowRuntimeService;
import com.plasticene.boot.flow.core.service.FlowTaskService;
import com.plasticene.boot.mybatis.core.utils.MybatisUtils;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author ZFJ
 * @date 2025/9/4
 */
@Service
public class FlowRuntimeServiceImpl extends ServiceImpl<FlowInstanceDAO, FlowInstance> implements FlowRuntimeService {
    @Resource
    private FlowInstanceDAO flowInstanceDAO;
    @Resource
    @Lazy
    private ProcessExecutor processExecutor;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private FlowDefinitionService flowDefinitionService;
    @Resource
    private CategoryService categoryService;
    @Resource
    @Lazy
    private FlowTaskService flowTaskService;
    @Resource
    private FlowOrganizationProvider flowOrganizationProvider;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public long startFlowInstanceById(Long definitionId, Long businessId, Map<String, Object> varMap) {
        FlowDefinition flowDefinition = flowDefinitionService.getStartableDefinition(definitionId);
        // 发布了的流程模型是合法的，可以直接使用，这里不需要再校验合法性，发布的时候已经校验过合法性了
        FlowNode flowNode = flowDefinition.getModelNode();
        FlowInstance instance = new FlowInstance();
        instance.setDefinitionId(definitionId);
        instance.setBusinessId(businessId);
        instance.setVarMap(varMap);
        instance.setCategory(flowDefinition.getCategory());
        instance.setOrgId(flowDefinition.getOrgId());
        instance.setStartTime(LocalDateTime.now());
        instance.setStatus(FlowInstanceStatusEnum.RUNNING.getCode());
        instance.setCurrentNodeKey(flowNode.getKey());
        instance.setCurrentNodeName(flowNode.getName());
        LoginUser loginUser = LoginUserHolder.get();
        instance.setUserId(loginUser.getId());
        flowInstanceDAO.insert(instance);

        // 发布流程实例开始事件
        InstanceEvent instanceEvent = buildInstanceEvent(instance, flowNode, FlowInstanceEventTypeEnum.START);
        applicationContext.publishEvent(instanceEvent);

        // 开始流转流程
        FlowParser.makeParentNode(flowNode);
        processExecutor.executeNode(instance, flowNode);

        return instance.getId();
    }

    @Override
    public List<FlowRouteNodeVO> calculateRoute(Long definitionId, Map<String, Object> varMap) {
        FlowDefinition flowDefinition = flowDefinitionService.getStartableDefinition(definitionId);
        FlowNode flowNode = flowDefinition.getModelNode();
        FlowParser.makeParentNode(flowNode);
        Map<String, Object> variables = varMap == null ? Map.of() : varMap;
        return processExecutor.calculateRouteTrace(flowNode, variables).stream()
                .map(this::toRouteNodeVO)
                .toList();
    }

    @Override
    public PageResult<FlowInstanceVO> page(FlowInstanceQuery query) {
        normalizeQuery(query);
        IPage<FlowInstanceVO> page = flowInstanceDAO.pageInstance(MybatisUtils.buildPage(query), query, true);
        List<FlowInstanceVO> records = page.getRecords();
        Map<String, String> categoryMap = categoryService.getCategoryMap();
        Map<Long, String> userMap = flowOrganizationProvider.getUserMap();
        Map<Long, LocalDateTime> nodeStartTimeMap = getCurrentNodeStartTimeMap(query.getOrgId(), records);
        LocalDateTime now = LocalDateTime.now();
        records.forEach(vo -> enrichInstanceVO(vo, categoryMap, userMap, nodeStartTimeMap.get(vo.getId()), now));
        return new PageResult<>(records, page.getTotal(), page.getPages());
    }

    @Override
    public FlowInstanceStatisticsVO statistics(FlowInstanceQuery query) {
        normalizeQuery(query);
        return flowInstanceDAO.statisticsInstance(query, false);
    }

    @Override
    public FlowInstanceDetailVO getInstanceDetail(Long instanceId) {
        if (instanceId == null) {
            throw new BizException("流程实例id不能为空");
        }
        LoginUser loginUser = LoginUserHolder.get();
        FlowInstance instance = flowInstanceDAO.selectById(instanceId);
        if (instance == null || !Objects.equals(instance.getOrgId(), loginUser.getOrgId())) {
            throw new BizException("流程实例不存在");
        }
        List<FlowTask> taskList = flowTaskService.listTaskByInstanceId(instanceId);
        FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
        if (definition == null || !Objects.equals(definition.getOrgId(), loginUser.getOrgId())) {
            throw new BizException("流程发布定义不存在");
        }

        Map<Long, String> userMap = flowOrganizationProvider.getUserMap();
        List<FlowTaskVO> taskVOList = taskList.stream()
                .map(task -> toTaskVO(task, userMap))
                .toList();
        Map<String, Object> variables = instance.getVarMap() == null ? Map.of() : instance.getVarMap();

        FlowInstanceVO instanceVO = toInstanceVO(instance, definition);
        LocalDateTime currentNodeStartTime = taskList.stream()
                .filter(task -> Objects.equals(task.getNodeKey(), instance.getCurrentNodeKey()))
                .map(FlowTask::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        enrichInstanceVO(instanceVO, categoryService.getCategoryMap(), userMap,
                currentNodeStartTime, LocalDateTime.now());

        FlowInstanceDetailVO detail = new FlowInstanceDetailVO();
        detail.setInstance(instanceVO);
        detail.setStartUserId(instance.getUserId());
        detail.setStartUserName(userMap.getOrDefault(instance.getUserId(), String.valueOf(instance.getUserId())));
        detail.setVersion(definition.getVersion());
        detail.setFormConfig(definition.getFormConfig());
        detail.setFormData(variables);
        detail.setModelNode(definition.getModelNode());
        detail.setRouteNodeKeys(processExecutor.calculateRouteTrace(definition.getModelNode(), variables).stream()
                .map(FlowNode::getKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        detail.setTasks(taskVOList);
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelInstance(Long instanceId) {
        if (instanceId == null) {
            throw new BizException("流程实例id不能为空");
        }
        LoginUser loginUser = LoginUserHolder.get();
        FlowInstance instance = selectInstanceForUpdate(instanceId);
        if (instance == null
                || !Objects.equals(instance.getOrgId(), loginUser.getOrgId())
                || !Objects.equals(instance.getUserId(), loginUser.getId())) {
            throw new BizException("流程实例不存在");
        }
        if (!Objects.equals(instance.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())) {
            throw new BizException("流程实例已结束，不能取消");
        }

        flowTaskService.cancelInstanceRunningTask(instanceId);
        endInstance(instance, resolveCurrentNode(instance), FlowInstanceStatusEnum.CANCEL);
    }

    @Override
    public FlowInstance selectInstanceForUpdate(Long instanceId) {
        return flowInstanceDAO.selectInstanceForUpdate(instanceId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateInstanceCurrentNode(Long instanceId, FlowNode currentNode) {
        FlowInstance instance = new FlowInstance();
        instance.setId(instanceId);
        instance.setCurrentNodeKey(currentNode.getKey());
        instance.setCurrentNodeName(currentNode.getName());
        flowInstanceDAO.updateById(instance);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void endInstance(FlowInstance instance, FlowNode currentNode, FlowInstanceStatusEnum status) {
        // 更新按照规范只更新必须的字段
        FlowInstance updateInstance = new FlowInstance();
        updateInstance.setId(instance.getId());
        updateInstance.setStatus(status.getCode());
        updateInstance.setEndTime(LocalDateTime.now());
        updateInstance.setCurrentNodeKey(currentNode.getKey());
        updateInstance.setCurrentNodeName(currentNode.getName());
        flowInstanceDAO.updateById(updateInstance);
        // 上下文instance需要传递最新状态
        instance.setStatus(status.getCode());
        // 发布流程结束事件
        InstanceEvent instanceEvent = buildInstanceEvent(instance, currentNode, FlowInstanceEventTypeEnum.END);
        applicationContext.publishEvent(instanceEvent);

    }

    private InstanceEvent buildInstanceEvent(FlowInstance instance, FlowNode currentNode,
                                             FlowInstanceEventTypeEnum eventType) {
        InstanceEvent instanceEvent = new InstanceEvent(this);
        instanceEvent.setInstance(instance);
        instanceEvent.setCurrentNode(currentNode);
        instanceEvent.setBusinessId(instance.getBusinessId());
        instanceEvent.setCategory(instance.getCategory());
        instanceEvent.setEventType(eventType);
        return instanceEvent;
    }

    private FlowRouteNodeVO toRouteNodeVO(FlowNode flowNode) {
        FlowRouteNodeVO vo = new FlowRouteNodeVO();
        vo.setKey(flowNode.getKey());
        vo.setName(flowNode.getName());
        vo.setType(flowNode.getType());
        vo.setShowText(flowNode.getShowText());
        vo.setApproveMode(flowNode.getApproveMode());
        return vo;
    }

    private FlowNode resolveCurrentNode(FlowInstance instance) {
        FlowDefinition definition = flowDefinitionService.getById(instance.getDefinitionId());
        if (definition != null) {
            FlowNode modelNode = definition.getModelNode();
            FlowParser.makeParentNode(modelNode);
            FlowNode currentNode = FlowParser.findNodeByKey(modelNode, instance.getCurrentNodeKey());
            if (currentNode != null) {
                return currentNode;
            }
        }
        FlowNode currentNode = new FlowNode();
        currentNode.setKey(instance.getCurrentNodeKey());
        currentNode.setName(instance.getCurrentNodeName());
        return currentNode;
    }

    private FlowInstanceStatusEnum getStatus(Integer code) {
        for (FlowInstanceStatusEnum status : FlowInstanceStatusEnum.values()) {
            if (Objects.equals(status.getCode(), code)) {
                return status;
            }
        }
        return null;
    }

    private FlowInstanceVO toInstanceVO(FlowInstance instance, FlowDefinition definition) {
        FlowInstanceVO vo = new FlowInstanceVO();
        vo.setId(instance.getId());
        vo.setDefinitionId(instance.getDefinitionId());
        vo.setBusinessId(instance.getBusinessId());
        vo.setName(definition.getName());
        vo.setCode(definition.getCode());
        vo.setCategory(instance.getCategory());
        vo.setStartUserId(instance.getUserId());
        vo.setCurrentNodeKey(instance.getCurrentNodeKey());
        vo.setCurrentNodeName(instance.getCurrentNodeName());
        vo.setStatus(instance.getStatus());
        vo.setStartTime(instance.getStartTime());
        vo.setEndTime(instance.getEndTime());
        return vo;
    }

    private void normalizeQuery(FlowInstanceQuery query) {
        query.setKeyword(StrUtil.trim(query.getKeyword()));
        query.setKeywordInstanceId(parseKeywordInstanceId(query.getKeyword()));
        query.setCategory(StrUtil.trim(query.getCategory()));
        query.setCurrentNodeName(StrUtil.trim(query.getCurrentNodeName()));
        if (query.getStartTimeBegin() != null && query.getStartTimeEnd() != null
                && query.getStartTimeBegin().isAfter(query.getStartTimeEnd())) {
            throw new BizException("发起时间开始值不能晚于结束值");
        }
    }

    private Long parseKeywordInstanceId(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return null;
        }
        String value = StrUtil.removePrefix(keyword, "#");
        try {
            long instanceId = Long.parseLong(value);
            return instanceId > 0 ? instanceId : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<Long, LocalDateTime> getCurrentNodeStartTimeMap(Long orgId, List<FlowInstanceVO> instances) {
        if (instances.isEmpty()) {
            return Map.of();
        }
        List<Long> instanceIds = instances.stream().map(FlowInstanceVO::getId).toList();
        List<FlowInstanceNodeTimeDTO> nodeTimes = flowInstanceDAO.listCurrentNodeStartTimes(orgId, instanceIds);
        Map<Long, LocalDateTime> result = new HashMap<>();
        nodeTimes.forEach(item -> result.put(item.getInstanceId(), item.getStartTime()));
        return result;
    }

    private void enrichInstanceVO(FlowInstanceVO vo,
                                  Map<String, String> categoryMap,
                                  Map<Long, String> userMap,
                                  LocalDateTime currentNodeStartTime,
                                  LocalDateTime now) {
        String category = vo.getCategory();
        vo.setCategoryName(category == null ? null : categoryMap.getOrDefault(category, category));
        Long startUserId = vo.getStartUserId();
        vo.setStartUserName(startUserId == null
                ? null
                : userMap.getOrDefault(startUserId, String.valueOf(startUserId)));
        FlowInstanceStatusEnum status = getStatus(vo.getStatus());
        vo.setStatusName(status == null ? null : status.getName());
        LoginUser loginUser = LoginUserHolder.get();
        vo.setCancelable(Objects.equals(vo.getStatus(), FlowInstanceStatusEnum.RUNNING.getCode())
                && startUserId != null
                && Objects.equals(startUserId, loginUser.getId()));
        LocalDateTime elapsedEndTime = vo.getEndTime() == null ? now : vo.getEndTime();
        vo.setElapsedTime(elapsedSeconds(vo.getStartTime(), elapsedEndTime));
        vo.setCurrentNodeElapsedTime(elapsedSeconds(currentNodeStartTime, elapsedEndTime));
    }

    private Long elapsedSeconds(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Math.max(0, Duration.between(startTime, endTime).getSeconds());
    }

    private FlowTaskVO toTaskVO(FlowTask task, Map<Long, String> userMap) {
        FlowTaskVO vo = new FlowTaskVO();
        vo.setId(task.getId());
        vo.setNodeKey(task.getNodeKey());
        vo.setNodeName(task.getNodeName());
        vo.setNodeType(task.getNodeType());
        vo.setAssignee(task.getAssignee());
        vo.setAssigneeName(task.getAssignee() == null
                ? "系统"
                : userMap.getOrDefault(task.getAssignee(), String.valueOf(task.getAssignee())));
        vo.setApproveType(task.getApproveType());
        vo.setApproveMode(task.getApproveMode());
        vo.setStatus(task.getStatus());
        FlowTaskStatusEnum status = getTaskStatus(task.getStatus());
        vo.setStatusName(status == null ? null : status.getName());
        vo.setStartTime(task.getStartTime());
        vo.setEndTime(task.getEndTime());
        vo.setComment(task.getComment());
        if (task.getStartTime() != null) {
            LocalDateTime endTime = task.getEndTime() == null ? LocalDateTime.now() : task.getEndTime();
            vo.setDurationMillis(Math.max(0, Duration.between(task.getStartTime(), endTime).toMillis()));
        }
        return vo;
    }

    private FlowTaskStatusEnum getTaskStatus(Integer code) {
        for (FlowTaskStatusEnum status : FlowTaskStatusEnum.values()) {
            if (Objects.equals(status.getCode(), code)) {
                return status;
            }
        }
        return null;
    }
}
