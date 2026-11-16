package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;

/**
 * 公开FlowTaskOperations完整用例。
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowTaskOperations {
    void approve(FlowTaskParam param, LoginUser actor);
    void reject(FlowTaskParam param, LoginUser actor);
    void saveForm(FlowTaskFormParam param, LoginUser actor);
    void addSign(FlowTaskAddSignParam param, LoginUser actor);
    void transfer(FlowTaskTransferParam param, LoginUser actor);
    void returnTask(FlowTaskReturnParam param, LoginUser actor);
    void withdraw(Long id, LoginUser actor);
    List<FlowTaskReturnTargetVO> listReturnTargets(Long id, LoginUser actor);
}
