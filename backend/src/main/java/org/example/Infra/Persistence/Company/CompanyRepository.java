package org.example.Infra.Persistence.Company;

import org.example.Core.Company.Model.Company;
import org.example.Infra.JdbcConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

public class CompanyRepository {
    public Company createWithOwner(String name, String ownerId) throws SQLException {
        String insertCompany = """
                INSERT INTO companies (name, owner_id)
                VALUES (?, ?)
                RETURNING id, name, owner_id, status, created_at
                """;
        String insertMember = """
                INSERT INTO company_members (company_id, user_id, member_role)
                VALUES (?, ?, 'OWNER')
                """;
        try (Connection conn = JdbcConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                Company company;
                try (PreparedStatement stmt = conn.prepareStatement(insertCompany)) {
                    stmt.setString(1, name);
                    stmt.setObject(2, UUID.fromString(ownerId));
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Failed to create company");
                        }
                        company = map(rs);
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement(insertMember)) {
                    stmt.setObject(1, UUID.fromString(company.id()));
                    stmt.setObject(2, UUID.fromString(ownerId));
                    stmt.executeUpdate();
                }
                conn.commit();
                return company;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public Optional<Company> findByUserId(String userId) throws SQLException {
        String sql = """
                SELECT c.id, c.name, c.owner_id, c.status, c.created_at
                FROM companies c
                JOIN company_members m ON m.company_id = c.id
                WHERE m.user_id = ?
                """;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(userId));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    /** Used by public pages and the worker: a suspended company's airdrops are frozen. */
    public boolean isActive(String companyId) throws SQLException {
        try (Connection conn = JdbcConnection.connect()) {
            return isActive(conn, companyId);
        }
    }

    public boolean isActive(Connection conn, String companyId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT status FROM companies WHERE id = ?")) {
            stmt.setObject(1, UUID.fromString(companyId));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && "ACTIVE".equals(rs.getString(1));
            }
        }
    }

    public Optional<String> findSuspendedReason(String companyId) throws SQLException {
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT suspended_reason FROM companies WHERE id = ?")) {
            stmt.setObject(1, UUID.fromString(companyId));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        }
    }

    private Company map(ResultSet rs) throws SQLException {
        return new Company(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("owner_id"),
                rs.getString("status"),
                iso(rs.getTimestamp("created_at"))
        );
    }

    static String iso(Timestamp ts) {
        return ts == null ? null : ts.toInstant().toString();
    }
}
