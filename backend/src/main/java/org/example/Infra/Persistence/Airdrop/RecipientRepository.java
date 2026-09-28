package org.example.Infra.Persistence.Airdrop;

import org.example.Core.Airdrop.Model.AirdropStats;
import org.example.Core.Airdrop.Model.Recipient;
import org.example.Core.Airdrop.Model.RecipientInput;
import org.example.Core.Airdrop.Model.RecipientStatus;
import org.example.Core.Airdrop.Validator.RecipientValidator;
import org.example.Infra.JdbcConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RecipientRepository {

    /**
     * Inserts recipients in one batch. Addresses already in this airdrop are skipped.
     * @return number of recipients actually inserted
     */
    public int insertBatch(Connection conn, String airdropId, List<RecipientInput> recipients) throws SQLException {
        String sql = """
                INSERT INTO airdrop_recipients (airdrop_id, address, address_key, amount)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (airdrop_id, address_key) DO NOTHING
                """;
        int inserted = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            UUID id = UUID.fromString(airdropId);
            for (RecipientInput r : recipients) {
                String address = r.address().trim();
                stmt.setObject(1, id);
                stmt.setString(2, address);
                stmt.setString(3, RecipientValidator.normalize(address));
                stmt.setBigDecimal(4, r.amount());
                stmt.addBatch();
            }
            for (int count : stmt.executeBatch()) {
                if (count > 0) {
                    inserted += count;
                }
            }
        }
        return inserted;
    }

    public List<Recipient> listByAirdrop(String airdropId, String status, int limit, int offset) throws SQLException {
        String sql = "SELECT id, airdrop_id, address, amount, status, tx_ref, error, updated_at " +
                "FROM airdrop_recipients WHERE airdrop_id = ?" +
                (status == null ? "" : " AND status = ?") +
                " ORDER BY created_at, id LIMIT ? OFFSET ?";
        List<Recipient> result = new ArrayList<>();
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 1;
            stmt.setObject(i++, UUID.fromString(airdropId));
            if (status != null) {
                stmt.setString(i++, status);
            }
            stmt.setInt(i++, limit);
            stmt.setInt(i, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public AirdropStats stats(String airdropId) throws SQLException {
        String sql = """
                SELECT status, COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS total
                FROM airdrop_recipients WHERE airdrop_id = ? GROUP BY status
                """;
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (RecipientStatus s : RecipientStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        long totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long cnt = rs.getLong("cnt");
                    byStatus.put(rs.getString("status"), cnt);
                    totalCount += cnt;
                    totalAmount = totalAmount.add(rs.getBigDecimal("total"));
                }
            }
        }
        return new AirdropStats(totalCount, totalAmount.stripTrailingZeros(), byStatus);
    }

    public long countByAirdrop(Connection conn, String airdropId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM airdrop_recipients WHERE airdrop_id = ?")) {
            stmt.setObject(1, UUID.fromString(airdropId));
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Claims up to {@code limit} pending recipients of PROCESSING airdrops and marks them PROCESSING.
     * FOR UPDATE SKIP LOCKED lets several workers run safely without picking the same row.
     */
    public List<Recipient> claimPending(Connection conn, int limit) throws SQLException {
        String sql = """
                UPDATE airdrop_recipients SET status = 'PROCESSING', attempts = attempts + 1
                WHERE id IN (
                    SELECT r.id FROM airdrop_recipients r
                    JOIN airdrops a ON a.id = r.airdrop_id
                    WHERE r.status = 'PENDING' AND a.status = 'PROCESSING'
                    ORDER BY r.created_at
                    LIMIT ?
                    FOR UPDATE OF r SKIP LOCKED)
                RETURNING id, airdrop_id, address, amount, status, tx_ref, error, updated_at
                """;
        List<Recipient> result = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public void markResult(String recipientId, RecipientStatus status, String txRef, String error) throws SQLException {
        String sql = "UPDATE airdrop_recipients SET status = ?, tx_ref = ?, error = ? WHERE id = ? AND status = 'PROCESSING'";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, txRef);
            stmt.setString(3, error);
            stmt.setObject(4, UUID.fromString(recipientId));
            stmt.executeUpdate();
        }
    }

    public int cancelOpen(Connection conn, String airdropId) throws SQLException {
        String sql = "UPDATE airdrop_recipients SET status = 'CANCELLED' WHERE airdrop_id = ? AND status = 'PENDING'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            return stmt.executeUpdate();
        }
    }

    /** Recipients stuck in PROCESSING (e.g. the server restarted mid-payout) go back to PENDING. */
    public int requeueStuck(int olderThanMinutes) throws SQLException {
        String sql = "UPDATE airdrop_recipients SET status = 'PENDING' " +
                "WHERE status = 'PROCESSING' AND updated_at < CURRENT_TIMESTAMP - (? * INTERVAL '1 minute')";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, olderThanMinutes);
            return stmt.executeUpdate();
        }
    }

    private Recipient map(ResultSet rs) throws SQLException {
        return new Recipient(
                rs.getString("id"),
                rs.getString("airdrop_id"),
                rs.getString("address"),
                rs.getBigDecimal("amount").stripTrailingZeros(),
                RecipientStatus.valueOf(rs.getString("status")),
                rs.getString("tx_ref"),
                rs.getString("error"),
                AirdropRepository.iso(rs.getTimestamp("updated_at"))
        );
    }
}
