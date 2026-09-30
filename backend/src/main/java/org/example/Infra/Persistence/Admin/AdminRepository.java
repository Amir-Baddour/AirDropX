package org.example.Infra.Persistence.Admin;

import org.example.Core.Admin.Model.AdminAction;
import org.example.Core.Admin.Model.CompanySummary;
import org.example.Core.Admin.Model.PlatformEvent;
import org.example.Core.Admin.Model.PlatformStats;
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

/** Cross-tenant queries. Only reachable from super-admin endpoints. */
public class AdminRepository {

    public PlatformStats stats() throws SQLException {
        try (Connection conn = JdbcConnection.connect()) {
            return new PlatformStats(
                    count(conn, "SELECT COUNT(*) FROM users"),
                    grouped(conn, "SELECT status, COUNT(*) FROM companies GROUP BY status", "ACTIVE", "SUSPENDED"),
                    grouped(conn, "SELECT status, COUNT(*) FROM airdrops GROUP BY status",
                            "DRAFT", "VALIDATED", "PROCESSING", "COMPLETED", "CANCELLED"),
                    grouped(conn, "SELECT status, COUNT(*) FROM claims GROUP BY status", "APPROVED", "NEEDS_REVIEW", "REJECTED"),
                    grouped(conn, "SELECT status, COUNT(*) FROM airdrop_recipients GROUP BY status",
                            "PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"),
                    count(conn, "SELECT COUNT(*) FROM claims WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '24 hours'")
            );
        }
    }

    public List<CompanySummary> listCompanies(String search, String status, int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.name, c.status, c.suspended_reason, c.suspended_at, c.created_at, u.username AS owner_username,
                       (SELECT COUNT(*) FROM company_members m WHERE m.company_id = c.id) AS members,
                       (SELECT COUNT(*) FROM airdrops a WHERE a.company_id = c.id) AS airdrops,
                       (SELECT COUNT(*) FROM claims cl JOIN airdrops a ON a.id = cl.airdrop_id WHERE a.company_id = c.id) AS claims
                FROM companies c
                LEFT JOIN users u ON u.id = c.owner_id
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND (c.name ILIKE ? OR u.username ILIKE ?)");
            String like = "%" + search.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            sql.append(" AND c.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY c.created_at DESC, c.id LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        List<CompanySummary> result = new ArrayList<>();
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new CompanySummary(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("status"),
                            rs.getString("owner_username"),
                            rs.getLong("members"),
                            rs.getLong("airdrops"),
                            rs.getLong("claims"),
                            rs.getString("suspended_reason"),
                            iso(rs.getTimestamp("suspended_at")),
                            iso(rs.getTimestamp("created_at"))));
                }
            }
        }
        return result;
    }

    public Optional<String> findCompanyName(Connection conn, String companyId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM companies WHERE id = ? FOR UPDATE")) {
            stmt.setObject(1, UUID.fromString(companyId));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    /** Optimistic guard: only changes the row if it is still in the expected status. */
    public boolean setCompanyStatus(Connection conn, String companyId, String from, String to, String reason) throws SQLException {
        String sql = "UPDATE companies SET status = ?, suspended_reason = ?, " +
                "suspended_at = CASE WHEN ? = 'SUSPENDED' THEN CURRENT_TIMESTAMP ELSE NULL END " +
                "WHERE id = ? AND status = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, to);
            stmt.setString(2, reason);
            stmt.setString(3, to);
            stmt.setObject(4, UUID.fromString(companyId));
            stmt.setString(5, from);
            return stmt.executeUpdate() == 1;
        }
    }

    public void addAudit(Connection conn, String adminId, String action, String targetType, String targetId, String detail) throws SQLException {
        String sql = "INSERT INTO admin_audit_log (admin_id, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(adminId));
            stmt.setString(2, action);
            stmt.setString(3, targetType);
            stmt.setString(4, targetId);
            stmt.setString(5, detail);
            stmt.executeUpdate();
        }
    }

    public List<AdminAction> listAudit(int limit) throws SQLException {
        String sql = """
                SELECT l.id, l.admin_id, u.username, l.action, l.target_type, l.target_id, l.detail, l.created_at
                FROM admin_audit_log l
                LEFT JOIN users u ON u.id = l.admin_id
                ORDER BY l.id DESC
                LIMIT ?
                """;
        List<AdminAction> result = new ArrayList<>();
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new AdminAction(rs.getLong("id"), rs.getString("admin_id"), rs.getString("username"),
                            rs.getString("action"), rs.getString("target_type"), rs.getString("target_id"),
                            rs.getString("detail"), iso(rs.getTimestamp("created_at"))));
                }
            }
        }
        return result;
    }

    /** Latest airdrop events across all companies. */
    public List<PlatformEvent> recentEvents(int limit) throws SQLException {
        String sql = """
                SELECT e.id, c.name AS company_name, a.id AS airdrop_id, a.name AS airdrop_name, e.type, e.message, e.created_at
                FROM airdrop_events e
                JOIN airdrops a ON a.id = e.airdrop_id
                JOIN companies c ON c.id = a.company_id
                ORDER BY e.id DESC
                LIMIT ?
                """;
        List<PlatformEvent> result = new ArrayList<>();
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new PlatformEvent(rs.getLong("id"), rs.getString("company_name"), rs.getString("airdrop_id"),
                            rs.getString("airdrop_name"), rs.getString("type"), rs.getString("message"),
                            iso(rs.getTimestamp("created_at"))));
                }
            }
        }
        return result;
    }

    private static long count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static Map<String, Long> grouped(Connection conn, String sql, String... keys) throws SQLException {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String k : keys) {
            map.put(k, 0L);
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getLong(2));
            }
        }
        return map;
    }

    private static String iso(Timestamp ts) {
        return ts == null ? null : ts.toInstant().toString();
    }
}
