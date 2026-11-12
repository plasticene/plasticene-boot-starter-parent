package com.plasticene.boot.flow.core.dao;

import com.plasticene.boot.flow.core.entity.FlowEventOutbox;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.StringTypeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证实际事件 Mapper 的顺序、租约、组织隔离及载荷映射契约。
 * @author ZFJ
 * @since 2026-11-12
 */
class FlowEventOutboxMapperTest {
    private Configuration configuration;

    @BeforeEach
    void loadMappers() throws Exception {
        configuration = new Configuration();
        for (String resource : List.of("mapper/FlowEventOutboxMapper.xml", "mapper/FlowInstanceMapper.xml")) {
            try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream);
                new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
    }

    @Test
    void shouldOnlyClaimTheFirstUnfinishedEventAndSkipLockedInstances() {
        String sql = sql("selectClaimCandidate");
        assertTrue(sql.contains("e.next_attempt_at <= ?"));
        assertTrue(sql.contains("e.lease_until <= ?"));
        assertTrue(sql.contains("p.instance_id = e.instance_id AND p.sequence_no < e.sequence_no"));
        assertTrue(sql.contains("p.status <> 'DELIVERED'"));
        assertTrue(sql.endsWith("LIMIT 1 FOR UPDATE SKIP LOCKED"));
        assertEquals(List.of("now", "now"), parameters(bound("selectClaimCandidate")));
    }

    @Test
    void shouldFenceRenewalAcknowledgementAndFailureByAnUnexpiredLeaseToken() {
        for (String id : List.of("renewLease", "markDelivered", "selectRetryCountForUpdate")) {
            String sql = sql(id);
            assertTrue(sql.contains("event_id = ?"), id);
            assertTrue(sql.contains("status = 'PROCESSING'"), id);
            assertTrue(sql.contains("lease_token = ?"), id);
            assertTrue(sql.contains("lease_until > ?"), id);
        }
        assertTrue(sql("selectRetryCountForUpdate").endsWith("FOR UPDATE"));
        assertTrue(sql("markFailed").contains("WHERE event_id = ? AND lease_token = ?"));
        assertTrue(sql("markFailed").contains("total_failures = total_failures + 1"));
    }

    @Test
    void shouldBindImmutablePayloadAsJsonTextWithoutEncodingItAgain() {
        var record = new FlowEventOutbox();
        record.setPayload("{\"eventId\":\"e\"}");
        BoundSql bound = configuration.getMappedStatement(FlowEventOutboxDAO.class.getName() + ".insertEvent")
                .getBoundSql(record);
        var payload = bound.getParameterMappings().stream().filter(p -> p.getProperty().equals("payload"))
                .findFirst().orElseThrow();
        assertInstanceOf(StringTypeHandler.class, payload.getTypeHandler());
        assertEquals(15, bound.getParameterMappings().size());
        var mapped = configuration.getResultMap(FlowEventOutboxDAO.class.getName() + ".eventResultMap")
                .getResultMappings();
        assertTrue(mapped.stream().anyMatch(m -> m.getColumn().equals("payload") && m.getProperty().equals("payload")));
        assertTrue(mapped.stream().anyMatch(m -> m.getColumn().equals("sequence_no") && m.getProperty().equals("sequenceNo")));
    }

    @Test
    void shouldRestrictFailureQueryAndManualRetryToTheRequestedOrganization() {
        assertTrue(sql("selectFailures").contains("WHERE org_id = ?"));
        assertFalse(sql("selectFailures").contains("payload"));
        assertEquals(List.of("orgId", "limit"), parameters(bound("selectFailures")));
        assertTrue(sql("retryPaused").contains("WHERE org_id = ? AND event_id = ? AND status = 'PAUSED'"));
        assertTrue(sql("retryPaused").contains("retry_count = 0"));
        for (String unchanged : List.of("payload", "sequence_no", "total_failures")) {
            assertFalse(sql("retryPaused").contains(unchanged));
        }
    }

    @Test
    void shouldOnlyDeleteDeliveredEventsOlderThanRetention() {
        assertEquals("DELETE FROM flow_event_outbox WHERE status = 'DELIVERED' AND delivered_at < ?",
                sql("deleteDeliveredBefore"));
        assertEquals(List.of("cutoff"), parameters(bound("deleteDeliveredBefore")));
    }

    @Test
    void shouldScopeInstanceSequenceAllocationByOrganization() {
        for (String id : List.of("incrementEventSequence", "selectEventSequence")) {
            var bound = configuration.getMappedStatement(FlowInstanceDAO.class.getName() + "." + id)
                    .getBoundSql(Map.of());
            assertTrue(bound.getSql().contains("WHERE id = ? AND org_id = ?"));
            assertEquals(List.of("instanceId", "orgId"), parameters(bound));
        }
    }

    private BoundSql bound(String id) {
        return configuration.getMappedStatement(FlowEventOutboxDAO.class.getName() + "." + id).getBoundSql(Map.of());
    }

    private String sql(String id) {
        return bound(id).getSql().replaceAll("\\s+", " ").trim();
    }

    private List<String> parameters(BoundSql bound) {
        return bound.getParameterMappings().stream().map(p -> p.getProperty()).toList();
    }
}
