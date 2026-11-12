package com.plasticene.boot.flow.core.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.plasticene.boot.flow.core.entity.FlowEventOutbox;
import com.plasticene.boot.mybatis.core.mapper.BaseMapperX;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 持久化流程事件并以行锁及领取令牌维护可靠交付状态。
 * @author ZFJ
 * @since 2026-11-12
 */
// 后台领取和清理跨组织执行；组织运维查询由 SQL 显式限定 orgId。
@InterceptorIgnore(tenantLine = "true")
public interface FlowEventOutboxDAO extends BaseMapperX<FlowEventOutbox> {
    /** 在调用方流程事务内保存事件载荷及初始交付状态。 */
    int insertEvent(FlowEventOutbox event);

    /** 锁定一个已到期的实例头部事件，跳过被其他工作者锁定的记录。 */
    FlowEventOutbox selectClaimCandidate(@Param("now") LocalDateTime now);

    /** 在领取事务持有行锁时设置领取令牌和期限。 */
    int markProcessing(@Param("eventId") String eventId, @Param("token") String token,
                       @Param("leaseUntil") LocalDateTime leaseUntil);

    /** 仅为仍持有有效领取令牌的工作者续期。 */
    int renewLease(@Param("eventId") String eventId, @Param("token") String token,
                   @Param("leaseUntil") LocalDateTime leaseUntil, @Param("now") LocalDateTime now);

    /** 仅由仍持有有效领取令牌的工作者确认交付成功。 */
    int markDelivered(@Param("eventId") String eventId, @Param("token") String token,
                      @Param("now") LocalDateTime now);

    /** 锁定当前有效领取记录并读取本轮失败计数；领取失效时返回空。 */
    Integer selectRetryCountForUpdate(@Param("eventId") String eventId, @Param("token") String token,
                                     @Param("now") LocalDateTime now);

    /** 在失败事务持有行锁时记录失败并释放领取，保留累计失败次数。 */
    int markFailed(@Param("eventId") String eventId, @Param("token") String token,
                   @Param("status") String status, @Param("retryCount") int retryCount,
                   @Param("nextAttemptAt") LocalDateTime nextAttemptAt, @Param("lastError") String lastError);

    /** 查询指定组织待重试或已暂停的失败事件，不读取事件载荷。 */
    List<FlowEventOutbox> selectFailures(@Param("orgId") Long orgId, @Param("limit") int limit);

    /** 恢复指定组织的暂停事件，重置本轮预算并保留原事件及累计失败信息。 */
    int retryPaused(@Param("orgId") Long orgId, @Param("eventId") String eventId,
                    @Param("now") LocalDateTime now);

    /** 只清理成功时间早于保留期限的事件。 */
    int deleteDeliveredBefore(@Param("cutoff") LocalDateTime cutoff);
}
