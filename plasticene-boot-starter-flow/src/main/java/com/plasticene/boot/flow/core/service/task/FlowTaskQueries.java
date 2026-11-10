package com.plasticene.boot.flow.core.service.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.dao.FlowTaskAddSignDAO;
import com.plasticene.boot.flow.core.dao.FlowTaskDAO;
import com.plasticene.boot.flow.core.entity.FlowTask;
import com.plasticene.boot.flow.core.entity.FlowTaskAddSign;
import com.plasticene.boot.flow.core.enums.FlowInstanceStatusEnum;
import com.plasticene.boot.flow.core.enums.FlowNodeEnum;
import com.plasticene.boot.flow.core.enums.FlowTaskStatusEnum;
import com.plasticene.boot.flow.core.model.query.FlowTaskQuery;
import com.plasticene.boot.flow.core.model.vo.FlowTaskPageVO;
import com.plasticene.boot.flow.core.provider.FlowOrganizationProvider;
import com.plasticene.boot.flow.core.service.FlowCategoryService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import com.plasticene.boot.mybatis.core.utils.MybatisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 查询流程任务及组装任务展示数据。
 * @author ZFJ
 * @since 2026-11-10
 */
@Service
public class FlowTaskQueries {
    @Resource
    private FlowTaskDAO flowTaskDAO;

    @Resource
    private FlowCategoryService flowCategoryService;

    @Resource
    private FlowOrganizationProvider flowOrganizationProvider;

    @Resource
    private FlowTaskAddSignDAO flowTaskAddSignDAO;

    @Resource
    private FlowTaskExecution flowTaskExecution;

    private static final List<Integer> DONE_STATUSES = List.of(
            FlowTaskStatusEnum.APPROVE.getCode(),
            FlowTaskStatusEnum.REJECT.getCode(),
            FlowTaskStatusEnum.CANCEL.getCode(),
            FlowTaskStatusEnum.TRANSFERRED.getCode(),
            FlowTaskStatusEnum.RETURNED.getCode()
    );
    private static final List<Integer> INSTANCE_STATUSES = Arrays.stream(FlowInstanceStatusEnum.values())
            .map(FlowInstanceStatusEnum::getCode)
            .toList();

    public PageResult<FlowTaskPageVO> pageMyTodo(FlowTaskQuery query) {
        query.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        query.setStatuses(List.of(FlowTaskStatusEnum.RUNNING.getCode(), FlowTaskStatusEnum.WAITING_ADD_SIGN.getCode()));
        return pageMyTask(query);
    }

    public PageResult<FlowTaskPageVO> pageMyDone(FlowTaskQuery query) {
        query.setNodeType(FlowNodeEnum.Type.APPROVE.getCode());
        List<Integer> statuses = query.getStatuses();
        if (CollUtil.isEmpty(statuses)) {
            query.setStatuses(DONE_STATUSES);
        } else {
            boolean invalidStatus = statuses.stream().anyMatch(status -> !DONE_STATUSES.contains(status));
            if (invalidStatus) {
                throw new BizException("已办任务状态仅支持已完成、拒绝、已取消、已转交或已退回");
            }
            query.setStatuses(statuses.stream().distinct().toList());
        }
        return pageMyTask(query);
    }

    public PageResult<FlowTaskPageVO> pageMyCopy(FlowTaskQuery query) {
        query.setNodeType(FlowNodeEnum.Type.COPY.getCode());
        query.setStatuses(List.of(FlowTaskStatusEnum.APPROVE.getCode()));
        return pageMyTask(query);
    }

    public List<FlowTaskAddSign> listAddSigns(Long instanceId) {
        return flowTaskAddSignDAO.selectList(new LambdaQueryWrapper<FlowTaskAddSign>()
                .eq(FlowTaskAddSign::getInstanceId, instanceId).orderByAsc(FlowTaskAddSign::getId));
    }

    public List<FlowTask> listTaskByInstanceId(Long instanceId) {
        PtcLambdaQueryWrapper<FlowTask> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId)
                .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL);
        queryWrapper.orderByAsc(FlowTask::getId);
        return flowTaskDAO.selectList(queryWrapper);
    }

    public List<FlowTask> runningApprovalTasks(Long instanceId) {
        return runningApprovalTasks(instanceId, null);
    }

    public List<FlowTask> runningApprovalTasks(Long instanceId, String nodeKey) {
        return flowTaskDAO.selectList(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getInstanceId, instanceId)
                .eq(nodeKey != null, FlowTask::getNodeKey, nodeKey)
                .eq(FlowTask::getNodeType, FlowNodeEnum.Type.APPROVE.getCode())
                .eq(FlowTask::getStatus, FlowTaskStatusEnum.RUNNING.getCode())
                .eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL)
                .isNull(FlowTask::getReturnTaskId));
    }

    public List<FlowTask> listTasksByInstanceIdAndNodeKey(Long instanceId, String nodeKey) {
        PtcLambdaQueryWrapper<FlowTask> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowTask::getInstanceId, instanceId);
        queryWrapper.eq(FlowTask::getNodeKey, nodeKey);
        queryWrapper.isNull(FlowTask::getReturnTaskId);
        queryWrapper.eq(FlowTask::getIsDelete, CommonConstant.IS_NOT_DEL);
        return flowTaskDAO.selectList(queryWrapper);
    }

    private PageResult<FlowTaskPageVO> pageMyTask(FlowTaskQuery query) {
        query.setKeyword(StrUtil.trim(query.getKeyword()));
        query.setCategory(StrUtil.trim(query.getCategory()));
        normalizeInstanceStatuses(query);
        validateTimeRange(query.getTaskStartTimeBegin(), query.getTaskStartTimeEnd(), "任务接收时间");
        validateTimeRange(query.getTaskEndTimeBegin(), query.getTaskEndTimeEnd(), "任务完成时间");

        LoginUser loginUser = flowTaskExecution.getLoginUser();
        query.setAssignee(loginUser.getId());
        query.setOrgId(loginUser.getOrgId());
        IPage<FlowTaskPageVO> page = flowTaskDAO.pageTask(MybatisUtils.buildPage(query), query);

        Map<String, String> categoryMap = flowCategoryService.getCategoryMap();
        Map<Long, String> userMap = flowOrganizationProvider.getUserMap();
        LocalDateTime now = LocalDateTime.now();
        page.getRecords().forEach(vo -> enrichTaskPageVO(vo, categoryMap, userMap, now));
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getPages());
    }

    private void validateTimeRange(LocalDateTime begin, LocalDateTime end, String fieldName) {
        if (begin != null && end != null && begin.isAfter(end)) {
            throw new BizException(fieldName + "开始值不能晚于结束值");
        }
    }

    private void enrichTaskPageVO(FlowTaskPageVO vo,
                                  Map<String, String> categoryMap,
                                  Map<Long, String> userMap,
                                  LocalDateTime now) {
        String category = vo.getCategory();
        vo.setCategoryName(category == null ? null : categoryMap.getOrDefault(category, category));
        Long startUserId = vo.getStartUserId();
        vo.setStartUserName(startUserId == null
                ? null
                : userMap.getOrDefault(startUserId, String.valueOf(startUserId)));
        FlowInstanceStatusEnum instanceStatus = getInstanceStatus(vo.getInstanceStatus());
        vo.setInstanceStatusName(instanceStatus == null ? null : instanceStatus.getName());
        if (vo.getTaskStartTime() == null) {
            return;
        }
        LocalDateTime endTime = vo.getTaskEndTime() == null ? now : vo.getTaskEndTime();
        vo.setElapsedTime(Math.max(0, Duration.between(vo.getTaskStartTime(), endTime).getSeconds()));
    }

    private void normalizeInstanceStatuses(FlowTaskQuery query) {
        List<Integer> statuses = query.getInstanceStatuses();
        if (CollUtil.isEmpty(statuses)) {
            query.setInstanceStatuses(null);
            return;
        }
        boolean invalidStatus = statuses.stream().anyMatch(status -> !INSTANCE_STATUSES.contains(status));
        if (invalidStatus) {
            throw new BizException("流程实例状态仅支持审批中、审批通过、审批拒绝或已取消");
        }
        query.setInstanceStatuses(statuses.stream().distinct().toList());
    }

    private FlowInstanceStatusEnum getInstanceStatus(Integer code) {
        return Arrays.stream(FlowInstanceStatusEnum.values())
                .filter(status -> Objects.equals(status.getCode(), code))
                .findFirst()
                .orElse(null);
    }
}
