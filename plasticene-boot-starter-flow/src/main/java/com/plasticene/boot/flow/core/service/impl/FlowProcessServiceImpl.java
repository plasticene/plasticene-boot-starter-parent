package com.plasticene.boot.flow.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plasticene.boot.common.constant.CommonConstant;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.user.RequestUserHolder;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowProcessDAO;
import com.plasticene.boot.flow.core.model.dto.ProcessNode;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.enums.FlowProcessStatusEnum;
import com.plasticene.boot.flow.core.model.param.FlowProcessParam;
import com.plasticene.boot.flow.core.model.vo.CategoryVO;
import com.plasticene.boot.flow.core.model.vo.FlowProcessVO;
import com.plasticene.boot.flow.core.parser.FlowParser;
import com.plasticene.boot.flow.core.service.CategoryService;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import com.plasticene.boot.mybatis.core.query.PtcLambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Service
public class FlowProcessServiceImpl extends ServiceImpl<FlowProcessDAO, FlowProcess> implements FlowProcessService {
    @Resource
    private FlowProcessDAO flowProcessDAO;
    @Resource
    private CategoryService categoryService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createFlowProcess(FlowProcessParam param) {
        String code = param.getCode();
        LoginUser loginUser = RequestUserHolder.getLoginUser();
        Long orgId = loginUser.getOrgId();
        boolean exist = existProcessByCode(orgId, code);
        if (exist) {
            throw new BizException("流程标识已存在!");
        }
        FlowProcess flowProcess = PtcBeanUtils.copy(param, FlowProcess.class);
        flowProcess.setOrgId(loginUser.getOrgId());
        flowProcessDAO.insert(flowProcess);
        return flowProcess.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long updateFlowProcess(FlowProcessParam param) {
        Long processId = param.getProcessId();
        FlowProcess flowProcess = flowProcessDAO.selectById(processId);
        Integer status = flowProcess.getStatus();
        // 当前流程为历史版本，不能修改，流程实例可能已经使用该版本流程模型
        if (Objects.equals(status, FlowProcessStatusEnum.HISTORY.getCode())) {
            throw new BizException("当前流程已是历史版本，不能修改");
        }
        // 当前流程草稿状态，可以随便改
        if (Objects.equals(status, FlowProcessStatusEnum.DRAFT.getCode())) {
            FlowProcess updateProcess = PtcBeanUtils.copy(param, FlowProcess.class);
            updateProcess.setId(processId);
            flowProcessDAO.updateById(updateProcess);
            return processId;
        }
        // 当前流程已发布状态，此时当前流程不动，基于原流程和传入的模型参数插入一条新的草稿流程
        // 防止多人同时基于已发布的流程编辑，产生一个流程多条草稿情况，先删除
        deleteDraftProcess(flowProcess.getOrgId(), flowProcess.getCode());
        PtcBeanUtils.copyPropertiesSkipExisting(flowProcess, param);
        FlowProcess insertProcess = PtcBeanUtils.copy(param, FlowProcess.class);
        insertProcess.setOrgId(flowProcess.getOrgId());
        flowProcessDAO.insert(insertProcess);
        return insertProcess.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void releaseFlowProcess(Long processId) {
        FlowProcess flowProcess = this.getById(processId);
        Integer status = flowProcess.getStatus();
        // 校验状态
        if (!Objects.equals(status, FlowProcessStatusEnum.DRAFT.getCode())) {
            throw new BizException("只有草稿状态的才能发布");
        }
        // 校验流程模型合法性
        ProcessNode processNode = flowProcess.getProcessNode();
        boolean valid = FlowParser.validateProcessNode(processNode);
        if (!valid) {
            throw new BizException("流程模型至少要有一个审批节点");
        }
        int version = 1;
        Long orgId = flowProcess.getOrgId();
        String code = flowProcess.getCode();
        FlowProcess releasedProcess = getReleasedProcessByCode(orgId, code);
        // 将之前发布的版本改为历史版本
        if (releasedProcess != null) {
            version = releasedProcess.getVersion() + 1;
            FlowProcess updateProcess = new FlowProcess();
            updateProcess.setId(releasedProcess.getId());
            updateProcess.setStatus(FlowProcessStatusEnum.HISTORY.getCode());
            updateProcess.setEnable(CommonConstant.IS_OFF);
            flowProcessDAO.updateById(updateProcess);
        }
        // 发布
        FlowProcess updateProcess = new FlowProcess();
        updateProcess.setId(processId);
        updateProcess.setStatus(FlowProcessStatusEnum.RELEASE.getCode());
        updateProcess.setReleaseTime(LocalDateTime.now());
        updateProcess.setVersion(version);
        updateProcess.setEnable(CommonConstant.IS_ON);
        flowProcessDAO.updateById(updateProcess);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void releaseFlowProcess(FlowProcessParam param) {
        Long releaseProcessId;
        if (Objects.isNull(param.getProcessId())) {
            // 当前流程模型没有保存过
            releaseProcessId = createFlowProcess(param);
        } else {
            // 当前流程模型保存过
            releaseProcessId = updateFlowProcess(param);
        }
        // 最终发布
        releaseFlowProcess(releaseProcessId);
    }

    @Override
    public List<CategoryVO> listFlowProcess(String processName) {
        List<CategoryVO> categoryList = categoryService.listCategory();
        LoginUser loginUser = RequestUserHolder.getLoginUser();
        PtcLambdaQueryWrapper<FlowProcess> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.likeIfPresent(FlowProcess::getName, processName);
        queryWrapper.eq(FlowProcess::getOrgId, loginUser.getOrgId());
        queryWrapper.in(FlowProcess::getStatus, List.of(FlowProcessStatusEnum.DRAFT.getCode(),
                FlowProcessStatusEnum.RELEASE.getCode()));
        queryWrapper.orderByDesc(FlowProcess::getId);
        List<FlowProcess> list = flowProcessDAO.selectList(queryWrapper);
        List<FlowProcessVO> processList = PtcBeanUtils.copyList(list, FlowProcessVO.class);
        // 有搜索，返回命中的分组及其流程
        if (StrUtil.isNotBlank(processName)) {
            if (CollUtil.isEmpty(processList)) {
                return new ArrayList<>();
            }
            Set<String> categorySet = processList.stream().map(FlowProcessVO::getCategory).collect(Collectors.toSet());
            categoryList = categoryList.stream().
                    filter(vo -> categorySet.contains(vo.getCode()))
                    .toList();
        }
        Map<String, List<FlowProcessVO>> processMap = processList.stream()
                .collect(Collectors.groupingBy(
                        FlowProcessVO::getCategory,
                        Collectors.toList()
                ));
        categoryList.forEach(category -> {
            List<FlowProcessVO> voList = processMap.getOrDefault(category.getCode(), List.of());
            category.setProcessList(voList);
        });
        return categoryList;
    }

    boolean existProcessByCode(Long orgId, String code) {
        PtcLambdaQueryWrapper<FlowProcess> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowProcess::getOrgId, orgId);
        queryWrapper.eq(FlowProcess::getCode, code);
        return this.exists(queryWrapper);
    }

    FlowProcess getReleasedProcessByCode(Long orgId, String code) {
        PtcLambdaQueryWrapper<FlowProcess> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowProcess::getOrgId, orgId);
        queryWrapper.eq(FlowProcess::getCode, code);
        queryWrapper.eq(FlowProcess::getStatus, FlowProcessStatusEnum.RELEASE.getCode());
        return this.getOne(queryWrapper);
    }

    void deleteDraftProcess(Long orgId, String code) {
        PtcLambdaQueryWrapper<FlowProcess> queryWrapper = new PtcLambdaQueryWrapper<>();
        queryWrapper.eq(FlowProcess::getOrgId, orgId);
        queryWrapper.eq(FlowProcess::getCode, code);
        queryWrapper.eq(FlowProcess::getStatus, FlowProcessStatusEnum.DRAFT.getCode());
        flowProcessDAO.delete(queryWrapper);
    }
}
