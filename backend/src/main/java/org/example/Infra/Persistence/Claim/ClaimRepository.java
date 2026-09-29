package org.example.Infra.Persistence.Claim;

import org.example.Core.Claim.Model.Claim;
import org.example.Core.Claim.Model.ClaimStatus;
import org.example.Core.Claim.Model.PublicClaimStatus;
import org.example.Core.Task.Model.TaskResult;
import org.example.Infra.JdbcConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClaimRepository {
    private static final String COLUMNS = "id, airdrop_id, address, status, reject_reason, created_at, reviewed_at";

    /** Returns the new claim id, or empty if this address already claimed this airdrop. */
    public Optional<String> insert(Connection conn, String airdropId, String address, String addressKey,
                                   ClaimStatus status, String tokenHash, String ipHash) throws SQLException {
        String sql = """
                INSERT INTO claims (airdrop_id, address, address_key, status, token_hash, ip_hash)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (airdrop_id, address_key) DO NOTHING
                RETURNING id
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            stmt.setString(2, address);
            stmt.setString(3, addressKey);
            stmt.setString(4, status.name());
            stmt.setString(5, tokenHash);
            stmt.setString(6, ipHash);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("id")) : Optional.empty();
            }
        }
    }

    public void insertResults(Connection conn, String claimId, List<TaskResult> results) throws SQLException {
        String sql = "INSERT INTO claim_task_results (claim_id, task_id, passed, needs_review, proof, detail) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (TaskResult r : results) {
                stmt.setObject(1, UUID.fromString(claimId));
                stmt.setObject(2, UUID.fromString(r.taskId()));
                stmt.setBoolean(3, r.passed());
                stmt.setBoolean(4, r.needsReview());
                stmt.setString(5, r.proof());
                stmt.setString(6, r.detail());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /** Claims that count toward the limit (approved or waiting for review). */
    public long countActive(Connection conn, String airdropId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM claims WHERE airdrop_id = ? AND status IN ('APPROVED', 'NEEDS_REVIEW')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public long countByIp(Connection conn, String airdropId, String ipHash) throws SQLException {
        String sql = "SELECT COUNT(*) FROM claims WHERE airdrop_id = ? AND ip_hash = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            stmt.setString(2, ipHash);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public Optional<Claim> findById(Connection conn, String airdropId, String claimId, boolean lock) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM claims WHERE id = ? AND airdrop_id = ?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(claimId));
            stmt.setObject(2, UUID.fromString(airdropId));
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Claim claim = map(rs, List.of());
                return Optional.of(withResults(conn, List.of(claim)).get(0));
            }
        }
    }

    public boolean updateStatus(Connection conn, String claimId, ClaimStatus from, ClaimStatus to,
                                String reviewerId, String rejectReason) throws SQLException {
        String sql = "UPDATE claims SET status = ?, reject_reason = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? AND status = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, to.name());
            stmt.setString(2, rejectReason);
            stmt.setObject(3, reviewerId == null ? null : UUID.fromString(reviewerId));
            stmt.setObject(4, UUID.fromString(claimId));
            stmt.setString(5, from.name());
            return stmt.executeUpdate() == 1;
        }
    }

    public List<Claim> listByAirdrop(String airdropId, String status, int limit, int offset) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM claims WHERE airdrop_id = ?" +
                (status == null ? "" : " AND status = ?") +
                " ORDER BY created_at, id LIMIT ? OFFSET ?";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 1;
            stmt.setObject(i++, UUID.fromString(airdropId));
            if (status != null) {
                stmt.setString(i++, status);
            }
            stmt.setInt(i++, limit);
            stmt.setInt(i, offset);
            List<Claim> claims = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    claims.add(map(rs, List.of()));
                }
            }
            return withResults(conn, claims);
        }
    }

    public Map<String, Long> countsByStatus(String airdropId) throws SQLException {
        String sql = "SELECT status, COUNT(*) FROM claims WHERE airdrop_id = ? GROUP BY status";
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ClaimStatus s : ClaimStatus.values()) {
            counts.put(s.name(), 0L);
        }
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString(1), rs.getLong(2));
                }
            }
        }
        return counts;
    }

    /** Status page for claimants: looked up by the hash of their private token. */
    public Optional<PublicClaimStatus> findPublicStatus(String tokenHash) throws SQLException {
        String sql = """
                SELECT a.name, a.token_symbol, c.address, c.status, c.reject_reason, r.status AS payout_status, r.tx_ref
                FROM claims c
                JOIN airdrops a ON a.id = c.airdrop_id
                LEFT JOIN airdrop_recipients r ON r.airdrop_id = c.airdrop_id AND r.address_key = c.address_key
                WHERE c.token_hash = ?
                """;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tokenHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new PublicClaimStatus(
                        rs.getString("name"),
                        rs.getString("token_symbol"),
                        rs.getString("address"),
                        ClaimStatus.valueOf(rs.getString("status")),
                        rs.getString("reject_reason"),
                        rs.getString("payout_status"),
                        rs.getString("tx_ref")
                ));
            }
        }
    }

    private List<Claim> withResults(Connection conn, List<Claim> claims) throws SQLException {
        if (claims.isEmpty()) {
            return claims;
        }
        Map<String, List<TaskResult>> byClaim = new LinkedHashMap<>();
        for (Claim c : claims) {
            byClaim.put(c.id(), new ArrayList<>());
        }
        String sql = "SELECT claim_id, task_id, passed, needs_review, proof, detail FROM claim_task_results WHERE claim_id = ANY(?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setArray(1, conn.createArrayOf("uuid", claims.stream().map(c -> UUID.fromString(c.id())).toArray()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    byClaim.get(rs.getString("claim_id")).add(new TaskResult(
                            rs.getString("task_id"),
                            rs.getBoolean("passed"),
                            rs.getBoolean("needs_review"),
                            rs.getString("proof"),
                            rs.getString("detail")));
                }
            }
        }
        List<Claim> result = new ArrayList<>();
        for (Claim c : claims) {
            result.add(new Claim(c.id(), c.airdropId(), c.address(), c.status(), c.rejectReason(),
                    c.createdAt(), c.reviewedAt(), byClaim.get(c.id())));
        }
        return result;
    }

    private Claim map(ResultSet rs, List<TaskResult> results) throws SQLException {
        return new Claim(
                rs.getString("id"),
                rs.getString("airdrop_id"),
                rs.getString("address"),
                ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("reject_reason"),
                iso(rs.getTimestamp("created_at")),
                iso(rs.getTimestamp("reviewed_at")),
                results
        );
    }

    private static String iso(Timestamp ts) {
        return ts == null ? null : ts.toInstant().toString();
    }
}
