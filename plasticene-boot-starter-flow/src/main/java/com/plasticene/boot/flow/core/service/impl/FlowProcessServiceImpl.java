package com.plasticene.boot.flow.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.plasticene.boot.common.utils.PtcBeanUtils;
import com.plasticene.boot.flow.core.dao.FlowProcessDAO;
import com.plasticene.boot.flow.core.entity.FlowProcess;
import com.plasticene.boot.flow.core.param.FlowProcessParam;
import com.plasticene.boot.flow.core.service.FlowProcessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ZFJ
 * @date 2025/9/2
 */
@Service
public class FlowProcessServiceImpl implements FlowProcessService {
    @Resource
    private FlowProcessDAO flowProcessDAO;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createFlowProcess(FlowProcessParam param) {
        FlowProcess flowProcess = PtcBeanUtils.copy(param, FlowProcess.class);
        flowProcess.setOrgId(0L);
        if (param.getProcessId() != null) {
            flowProcess.setModel(JSON.toJSONString(param.getProcessId()));
        }
        flowProcessDAO.insert(flowProcess);
        return flowProcess.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateFlowProcess(FlowProcessParam param) {
        FlowProcess flowProcess = PtcBeanUtils.copy(param, FlowProcess.class);
        flowProcess.setId(param.getProcessId());
        flowProcessDAO.updateById(flowProcess);
    }
}
