package com.plasticene.boot.flow.core.api.service;

import com.plasticene.boot.common.user.LoginUser;
import com.plasticene.boot.flow.core.api.vo.FlowEventFailureVO;
import java.util.List;

/**
 * 流程事件投递失败查询与人工重试的公开接口。
 * <p>所有方法均校验宿主提供的可信操作者、组织归属及相应权限。</p>
 * @author ZFJ
 * @since 2026-11-16
 */
public interface FlowEventApi {
    /**
     * 查询当前组织的失败事件投递记录，要求具备事件查询权限。
     *
     * @param limit 最多返回的记录数量
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     * @return 失败投递记录列表，不包含事件载荷
     */
    List<FlowEventFailureVO> failures(int limit, LoginUser actor);
    /**
     * 将当前组织处于暂停状态的事件重新置为待投递，要求具备事件重试权限。
     *
     * @param eventId 待重试的事件 ID
     * @param actor 宿主提供的可信操作者，须包含用户及组织身份
     */
    void retry(String eventId, LoginUser actor);
}
