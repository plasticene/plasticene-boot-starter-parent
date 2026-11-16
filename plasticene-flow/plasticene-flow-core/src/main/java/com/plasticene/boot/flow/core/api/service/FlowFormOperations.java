package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;

/**
 * 公开FlowFormOperations完整用例。
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowFormOperations {
    Long create(FlowFormParam param, LoginUser actor);
    void update(FlowFormParam param, LoginUser actor);
    void delete(List<Long> ids, LoginUser actor);
    List<FlowFormVO> list(LoginUser actor);
    PageResult<FlowFormVO> page(FlowFormQuery query, LoginUser actor);
    FlowFormVO get(Long id, LoginUser actor);
}
