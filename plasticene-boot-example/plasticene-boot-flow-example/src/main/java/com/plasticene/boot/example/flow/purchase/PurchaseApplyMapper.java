package com.plasticene.boot.example.flow.purchase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 沿用示例应用的 JDBC 能力读写采购申请表。
 * @author ZFJ
 * @since 2026-11-18
 */
@Repository
public class PurchaseApplyMapper {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public PurchaseApplyMapper(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** 插入申请；重复请求保持已保存的数据不变，由后续加锁查询判定结果。 */
    public void insertIfAbsent(PurchaseApplyParam param, Long orgId, Long userId) {
        jdbc.update("""
                INSERT INTO purchase_apply
                    (org_id, request_no, title, description, amount, category, purposes, urgent,
                     expected_date, status, created_by, created_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE id = id
                """, orgId, param.getRequestNo(), param.getTitle(), param.getDescription(), param.getAmount(),
                param.getCategory(), writePurposes(param.getPurposes()), param.getUrgent(),
                param.getExpectedDate(), userId);
    }

    /** 锁定同一组织内的提交请求，串行处理并发重试。 */
    public PurchaseApply lockByRequest(Long orgId, String requestNo) {
        return jdbc.query("SELECT * FROM purchase_apply WHERE org_id = ? AND request_no = ? FOR UPDATE",
                this::read, orgId, requestNo).stream().findFirst().orElse(null);
    }

    /** 根据组织和申请 ID 查询单据，可在事件事务中加行锁。 */
    public PurchaseApply find(Long orgId, Long id, boolean lock) {
        return jdbc.query("SELECT * FROM purchase_apply WHERE org_id = ? AND id = ?" + (lock ? " FOR UPDATE" : ""),
                this::read, orgId, id).stream().findFirst().orElse(null);
    }

    /** 关联新建的流程实例，保留回调负责更新的审批状态。 */
    public void bindInstance(Long orgId, Long id, Long instanceId) {
        jdbc.update("UPDATE purchase_apply SET instance_id = ? WHERE org_id = ? AND id = ? AND instance_id IS NULL",
                instanceId, orgId, id);
    }

    /** 只允许从审批中进入终态，返回实际更新行数。 */
    public int finish(Long orgId, Long id, Long instanceId, Integer status) {
        return jdbc.update("""
                UPDATE purchase_apply SET status = ?
                WHERE org_id = ? AND id = ? AND instance_id = ? AND status = 0
                """, status, orgId, id, instanceId);
    }

    /** 分页查询当前组织内指定申请人的采购单。 */
    public List<PurchaseApply> page(Long orgId, Long userId, int offset, int size) {
        return jdbc.query("""
                SELECT * FROM purchase_apply WHERE org_id = ? AND created_by = ?
                ORDER BY created_time DESC, id DESC LIMIT ? OFFSET ?
                """, this::read, orgId, userId, size, offset);
    }

    /** 统计当前申请人的采购单数量。 */
    public long count(Long orgId, Long userId) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM purchase_apply WHERE org_id = ? AND created_by = ?",
                Long.class, orgId, userId);
        return total == null ? 0 : total;
    }

    private String writePurposes(List<String> purposes) {
        try {
            return json.writeValueAsString(purposes);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("无法保存采购用途", failure);
        }
    }

    private PurchaseApply read(ResultSet row, int index) throws SQLException {
        var apply = new PurchaseApply();
        apply.setId(row.getLong("id"));
        apply.setOrgId(row.getLong("org_id"));
        apply.setRequestNo(row.getString("request_no"));
        apply.setTitle(row.getString("title"));
        apply.setDescription(row.getString("description"));
        apply.setAmount(row.getBigDecimal("amount"));
        apply.setCategory(row.getString("category"));
        try {
            apply.setPurposes(json.readValue(row.getString("purposes"), new TypeReference<List<String>>() { }));
        } catch (JsonProcessingException failure) {
            throw new SQLException("无法读取采购用途", failure);
        }
        apply.setUrgent(row.getBoolean("urgent"));
        apply.setExpectedDate(row.getDate("expected_date").toLocalDate());
        long instanceId = row.getLong("instance_id");
        apply.setInstanceId(row.wasNull() ? null : instanceId);
        apply.setStatus(row.getInt("status"));
        apply.setCreatedBy(row.getLong("created_by"));
        apply.setCreatedTime(row.getTimestamp("created_time").toLocalDateTime());
        return apply;
    }
}
