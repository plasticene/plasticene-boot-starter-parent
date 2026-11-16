package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.common.pojo.PageResult;
import com.plasticene.boot.flow.core.api.param.*;
import com.plasticene.boot.flow.core.api.query.*;
import com.plasticene.boot.flow.core.api.vo.*;
import com.plasticene.boot.flow.core.model.FlowVariableSchema;
import java.util.List;

/**
 * 公开FlowEventOperations完整用例。
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowEventOperations {
    List<FlowEventFailureVO> failures(int limit, LoginUser actor);
    void retry(String eventId, LoginUser actor);
}
