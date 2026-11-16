package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;

/**
 * 公开FlowModelOperations完整用例。
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowModelOperations {
    Long create(FlowModelParam param, LoginUser actor);
    void update(FlowModelParam param, LoginUser actor);
    void delete(List<Long> ids, LoginUser actor);
    FlowModelVO get(Long id, LoginUser actor);
    PageResult<FlowModelVO> page(FlowModelQuery query, LoginUser actor);
    FlowModelStatisticsVO statistics(FlowModelQuery query, LoginUser actor);
    void setEnabled(FlowModelEnableParam param, LoginUser actor);
    void publish(Long id, LoginUser actor);
    List<FlowDefinitionVO> listVersions(Long id, LoginUser actor);
    FlowDefinitionVO getVersion(Long id, Long definitionId, LoginUser actor);
    FlowVariableSchema getVariableSchema(Long id, LoginUser actor);
}
