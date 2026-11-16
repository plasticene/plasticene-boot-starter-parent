package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;

/**
 * 公开FlowCategoryOperations完整用例。
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowCategoryOperations {
    Long create(FlowCategoryParam param, LoginUser actor);
    void update(FlowCategoryParam param, LoginUser actor);
    void delete(List<Long> ids, LoginUser actor);
    List<FlowCategoryVO> list(LoginUser actor);
    PageResult<FlowCategoryVO> page(FlowCategoryQuery query, LoginUser actor);
    void sort(List<Long> ids, LoginUser actor);
}
