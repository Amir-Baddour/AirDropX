package org.example.Infra.Persistence.Airdrop;

import org.example.Core.Airdrop.Model.Airdrop;
import org.example.Core.Airdrop.Model.AirdropStatus;
import org.example.Infra.JdbcConnection;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AirdropRepository {
    private static final String COLUMNS =
            "id, company_id, name, description, token_symbol, status, created_by, created_at, updated_at, launched_at, completed_at";

    public Airdrop create(String companyId, String name, String description, String tokenSymbol, String createdBy) throws SQLException {
        String sql = "INSERT INTO airdrops (company_id, name, description, token_symbol, created_by) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING " + COLUMNS;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(companyId));
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setString(4, tokenSymbol);
            stmt.setObject(5, UUID.fromString(createdBy));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
            throw new SQLException("Failed to create airdrop");
        }
    }

    public Optional<Airdrop> findByIdAndCompany(String airdropId, String companyId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM airdrops WHERE id = ? AND company_id = ?";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            stmt.setObject(2, UUID.fromString(companyId));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Airdrop> listByCompany(String companyId, int limit, int offset) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM airdrops WHERE company_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Airdrop> result = new ArrayList<>();
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(companyId));
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    /**
     * Moves an airdrop to a new status only if it is currently in one of the expected statuses.
     * The WHERE clause makes this safe against concurrent requests (optimistic guard).
     */
    public boolean transition(Connection conn, String airdropId, Set<AirdropStatus> from, AirdropStatus to) throws SQLException {
        String extra = switch (to) {
            case PROCESSING -> ", launched_at = CURRENT_TIMESTAMP";
            case COMPLETED, CANCELLED -> ", completed_at = CURRENT_TIMESTAMP";
            default -> "";
        };
        String sql = "UPDATE airdrops SET status = ?" + extra + " WHERE id = ? AND status = ANY(?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            Array fromArray = conn.createArrayOf("varchar", from.stream().map(Enum::name).toArray());
            stmt.setString(1, to.name());
            stmt.setObject(2, UUID.fromString(airdropId));
            stmt.setArray(3, fromArray);
            return stmt.executeUpdate() == 1;
        }
    }

    /** Airdrops that are PROCESSING but have no recipients left to send. */
    public List<String> findFinishedProcessingIds(Connection conn) throws SQLException {
        String sql = """
                SELECT a.id FROM airdrops a
                WHERE a.status = 'PROCESSING'
                  AND NOT EXISTS (
                      SELECT 1 FROM airdrop_recipients r
                      WHERE r.airdrop_id = a.id AND r.status IN ('PENDING', 'PROCESSING'))
                """;
        List<String> ids = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getString("id"));
            }
        }
        return ids;
    }

    public Optional<String> findTokenSymbol(String airdropId) throws SQLException {
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT token_symbol FROM airdrops WHERE id = ?")) {
            stmt.setObject(1, UUID.fromString(airdropId));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    private Airdrop map(ResultSet rs) throws SQLException {
        return new Airdrop(
                rs.getString("id"),
                rs.getString("company_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("token_symbol"),
                AirdropStatus.valueOf(rs.getString("status")),
                rs.getString("created_by"),
                iso(rs.getTimestamp("created_at")),
                iso(rs.getTimestamp("updated_at")),
                iso(rs.getTimestamp("launched_at")),
                iso(rs.getTimestamp("completed_at"))
        );
    }

    static String iso(Timestamp ts) {
        return ts == null ? null : ts.toInstant().toString();
    }
}
