package org.example.Infra.Persistence.User;

import org.example.Core.User.Exception.EmailAlreadyRegisteredException;
import org.example.Core.User.Model.UserProfile;
import org.example.Infra.JdbcConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/** Queries for email/password accounts and profile data. Google sign-in keeps using UserRepository. */
public class AccountRepository {
    public record Credentials(String userId, String passwordHash) {
    }

    private static final String PROFILE_SELECT = """
            SELECT u.id::text AS id, u.username, u.provider, u.email, u.first_name, u.last_name, u.phone,
                   u.address, u.pfp, r.name AS role_name, (u.password_hash IS NOT NULL) AS has_password, u.created_at
            FROM users u LEFT JOIN roles r ON u.role_id = r.id
            """;

    /** Creates a LOCAL user with the default USER role (id 1, same as Google sign-ups) and returns its id. */
    public String createLocalUser(String email, String passwordHash, String firstName, String lastName)
            throws SQLException, EmailAlreadyRegisteredException {
        String sql = "INSERT INTO users (username, provider, provider_id, email, password_hash, first_name, last_name, role_id) "
                + "VALUES (?, 'LOCAL', ?, ?, ?, ?, ?, 1) RETURNING id::text";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, email);
            stmt.setString(3, email);
            stmt.setString(4, passwordHash);
            stmt.setString(5, firstName);
            stmt.setString(6, lastName);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) { // unique_violation on the username or the email index
                throw new EmailAlreadyRegisteredException("An account with this email already exists");
            }
            throw e;
        }
    }

    public Optional<Credentials> findCredentialsByEmail(String email) throws SQLException {
        String sql = "SELECT id::text, password_hash FROM users WHERE provider = 'LOCAL' AND LOWER(email) = LOWER(?)";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(new Credentials(rs.getString(1), rs.getString(2))) : Optional.empty();
            }
        }
    }

    public Optional<UserProfile> findProfile(String userId) throws SQLException {
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(PROFILE_SELECT + " WHERE u.id = ?::uuid")) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void updateProfile(String userId, String firstName, String lastName, String phone, String address) throws SQLException {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, phone = ?, address = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ?::uuid";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, phone);
            stmt.setString(4, address);
            stmt.setString(5, userId);
            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Profile update failed, no rows affected.");
            }
        }
    }

    public Optional<String> findPasswordHash(String userId) throws SQLException {
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT password_hash FROM users WHERE id = ?::uuid")) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public void updatePasswordHash(String userId, String passwordHash) throws SQLException {
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?::uuid")) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, userId);
            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Password update failed, no rows affected.");
            }
        }
    }

    private UserProfile map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return new UserProfile(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("provider"),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("pfp"),
                rs.getString("role_name"),
                rs.getBoolean("has_password"),
                created == null ? null : created.toInstant().toString()
        );
    }
}
