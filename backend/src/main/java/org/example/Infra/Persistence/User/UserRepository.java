package org.example.Infra.Persistence.User;
import org.example.Core.Authentication.Model.Token;
import org.example.Core.Role.Model.Role;
import org.example.Core.User.Model.User;
import org.example.Infra.JdbcConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class UserRepository {
    public User findUserByProvider(String provider, String username) throws SQLException {
        String sql = """
               SELECT u.id, u.username, u.provider, u.provider_id, u.pfp, u.address,
                r.id as role_id, r.name as role_name,
                t.id as token_id, t.refresh_token, t.expires_at
               FROM users u
               LEFT JOIN roles r ON u.role_id = r.id
               LEFT JOIN user_tokens t ON u.id = t.user_id
               WHERE u.provider = ? AND u.username = ?
                """;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, provider);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }
    public User createUser(String email, String username, String provider, String providerId, String pfp, String address) throws SQLException {
        String sql = "WITH inserted_users AS (" +
                "INSERT INTO users (username, provider, provider_id, pfp, address, role_id) " +
                "VALUES (?, ?, ?, ?, ?, 1) " +
                "RETURNING id, username, provider, provider_id, pfp, address, role_id " +
                ") SELECT iu.id, iu.role_id, iu.username, iu.provider, iu.provider_id, iu.pfp, iu.address, r.name FROM " +
                "inserted_users as iu LEFT JOIN roles r ON iu.role_id = r.id";

        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, provider);
            stmt.setString(3, providerId);
            stmt.setString(4, pfp);
            stmt.setString(5, address);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Role userRole = new Role(
                        rs.getString("role_id"),
                        rs.getString("name")
                );
                return new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("provider"),
                        rs.getString("provider_id"),
                        rs.getString("pfp"),
                        rs.getString("address"),
                        userRole,
                        null
                );
            }
            throw new SQLException("Failed to create user");
        }
    }
    public void updateUser(String userId, String username, String pfp, String address) throws SQLException {
        String sql = "UPDATE users SET username = ?, pfp = ?, address = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?::uuid";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, pfp);
            stmt.setString(3, address);
            stmt.setString(4, userId);
            int updatedRows = stmt.executeUpdate();

            if (updatedRows == 0) {
                throw new SQLException("User update failed, no rows affected.");
            }
        }
    }
    public void updateUserRole(String userId, String roleId) throws SQLException {
        String sql = "UPDATE users SET role_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?::uuid";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(roleId));  // role_id is INTEGER in the schema
            stmt.setString(2, userId);
            int updatedRows = stmt.executeUpdate();

            if (updatedRows == 0) {
                throw new SQLException("User role update failed, no rows affected.");
            }
        }
    }
    public User findUserById(String userId) throws SQLException {
        String sql = """
                SELECT u.id, u.username, u.provider, u.provider_id, u.pfp, u.address, 
                       r.id as role_id, r.name as role_name,
                       t.id as token_id, t.refresh_token, t.expires_at
                FROM users u
                LEFT JOIN roles r ON u.role_id = r.id
                LEFT JOIN user_tokens t ON u.id = t.user_id
                WHERE u.id = ?::uuid
            """;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        Role role = null;
        if (rs.getString("role_id") != null) {
            role = new Role(
                    rs.getString("role_id"),
                    rs.getString("role_name")
            );
        }
        Token token = null;
        if (rs.getString("token_id") != null) {
            token = new Token(
                    rs.getString("token_id"),
                    rs.getTimestamp("expires_at").getTime()
            );
        }
        return new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("provider"),
                rs.getString("address"),
                rs.getString("pfp"),
                rs.getString("address"),
                role,
                token
        );
    }
}