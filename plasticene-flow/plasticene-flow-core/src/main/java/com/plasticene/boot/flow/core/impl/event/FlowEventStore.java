package com.plasticene.boot.flow.core.impl.event;

import com.plasticene.boot.flow.core.api.event.FlowEvent;
import com.plasticene.boot.flow.core.api.event.FlowInstanceEvent;
import com.plasticene.boot.flow.core.api.event.FlowTaskEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasticene.boot.flow.core.config.FlowEventProperties;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowEventOutboxDAO;
import com.plasticene.boot.flow.core.impl.persistence.dao.FlowInstanceDAO;
import com.plasticene.boot.flow.core.impl.persistence.entity.FlowEventOutbox;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 在宿主事务内记录流程事件并维护有序交付状态。
 * @author ZFJ
 * @since 2026-11-11
 */
public class FlowEventStore implements FlowEventPublisher {
    private final FlowEventOutboxDAO eventDAO;
    private final FlowInstanceDAO instanceDAO;
    private final DataSource dataSource;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final FlowEventProperties properties;
    private final Clock clock;

    public FlowEventStore(DataSource dataSource, PlatformTransactionManager transactionManager,
                          FlowEventOutboxDAO eventDAO, FlowInstanceDAO instanceDAO,
                          ObjectMapper json, FlowEventProperties properties, Clock clock) {
        this.dataSource = dataSource;
        this.eventDAO = eventDAO;
        this.instanceDAO = instanceDAO;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.json = json;
        this.properties = properties;
        this.clock = clock;
    }

    /** 将实例序号和事件记录写入宿主当前流程事务，不单独提交。 */
    @Override
    public void publish(FlowEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.hasResource(dataSource)) {
            throw new IllegalStateException("流程事件必须在流程变更事务中持久化");
        }
        FlowEventConsumers.normalize(event.category());
        if (instanceDAO.incrementEventSequence(event.orgId(), event.instanceId()) != 1) {
            throw new IllegalStateException("事件所属流程实例不存在");
        }
        Long sequence = instanceDAO.selectEventSequence(event.orgId(), event.instanceId());
        FlowEventOutbox record = new FlowEventOutbox();
        record.setEventId(event.eventId());
        record.setOrgId(event.orgId());
        record.setInstanceId(event.instanceId());
        record.setSequenceNo(sequence);
        record.setCategory(event.category());
        record.setBusinessId(event.businessId());
        record.setDefinitionId(event.definitionId());
        record.setEventType(event.eventType().name());
        record.setEventTime(event.eventTime());
        try {
            record.setPayload(json.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("无法序列化流程事件", ex);
        }
        record.setStatus("PENDING");
        record.setRetryCount(0);
        record.setTotalFailures(0L);
        record.setNextAttemptAt(now());
        record.setCreatedAt(now());
        eventDAO.insertEvent(record);
    }

    /** 在独立 READ_COMMITTED 短事务中锁定并领取实例最早的未完成事件。 */
    public Claimed claim() {
        return transactions.execute(status -> {
            FlowEventOutbox candidate = eventDAO.selectClaimCandidate(now());
            if (candidate == null) {
                return null;
            }
            Claimed event = new Claimed(candidate.getEventId(), candidate.getEventType(), candidate.getPayload(),
                    UUID.randomUUID().toString());
            eventDAO.markProcessing(event.eventId(), event.token(), now().plus(properties.getLease()));
            return event;
        });
    }

    /** 按原事件类型反序列化不可变载荷。 */
    public FlowEvent decode(Claimed event) throws JsonProcessingException {
        Class<? extends FlowEvent> type = event.eventType().startsWith("INSTANCE_")
                ? FlowInstanceEvent.class : FlowTaskEvent.class;
        return json.readValue(event.payload(), type);
    }

    /** 仅在当前令牌仍有效且未过期时延长领取期限。 */
    public boolean renew(Claimed event) {
        return eventDAO.renewLease(event.eventId(), event.token(), now().plus(properties.getLease()), now()) == 1;
    }

    /** 业务正常返回后，以当前有效令牌确认交付成功。 */
    public boolean delivered(Claimed event) {
        return eventDAO.markDelivered(event.eventId(), event.token(), now()) == 1;
    }

    /** 在独立短事务内记录失败、计算退避或暂停；过期工作者不再写入。 */
    public void failed(Claimed event, Throwable failure) {
        transactions.executeWithoutResult(status -> {
            Integer used = eventDAO.selectRetryCountForUpdate(event.eventId(), event.token(), now());
            if (used == null) {
                return;
            }
            String state = used >= properties.getMaxRetries() ? "PAUSED" : "PENDING";
            String message = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
            eventDAO.markFailed(event.eventId(), event.token(), state, used + 1,
                    now().plus(properties.retryDelay(used)), message.substring(0, Math.min(message.length(), 1000)));
        });
    }

    /** 查询指定组织的失败诊断，不向运维接口返回事件载荷。 */
    public List<FlowEventDelivery> failures(Long orgId, int limit) {
        return eventDAO.selectFailures(orgId, Math.clamp(limit, 1, 100)).stream()
                .map(record -> new FlowEventDelivery(record.getEventId(), record.getOrgId(), record.getInstanceId(),
                        record.getCategory(), record.getEventType(), record.getStatus(), record.getRetryCount(),
                        record.getLastError(), record.getNextAttemptAt()))
                .toList();
    }

    /** 仅恢复指定组织的暂停事件，保留事件身份、载荷和累计失败次数。 */
    public boolean retry(Long orgId, String eventId) {
        return eventDAO.retryPaused(orgId, eventId, now()) == 1;
    }

    /** 只删除超过保留期限且已成功交付的记录。 */
    public int cleanup() {
        return eventDAO.deleteDeliveredBefore(now().minus(properties.getRetention()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 一次领取的载荷及用于拒绝过期确认的令牌。
     * @author ZFJ
     * @since 2026-11-11
     */
    public record Claimed(String eventId, String eventType, String payload, String token) {
    }
}
