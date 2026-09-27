package org.example.Infra.Persistence.UserToken;
import org.example.Core.User.UserToken.Model.UserToken;
import org.example.Infra.JdbcConnection;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;
public class UserTokenRepository {
    public Optional<UserToken> findTokenByUserId(String userId) throws SQLException {
        String sql = """
                    SELECT id, user_id, refresh_token, expires_at, created_at, updated_at
                    FROM user_tokens
                    WHERE user_id = ?
                    """;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(userId));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToToken(rs));
            }
        }
        return Optional.empty();
    }
    public UserToken createToken(String userId, String refreshToken, Timestamp expiresAt) throws SQLException {
        String sql = """
                    INSERT INTO user_tokens (user_id, refresh_token, expires_at)
                    VALUES (?, ?, ?)
                    RETURNING id, user_id, refresh_token, expires_at, created_at, updated_at
                    """;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(userId));  // Convert string to UUID
            stmt.setString(2, refreshToken);
            stmt.setTimestamp(3, expiresAt);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToToken(rs);
            }
            throw new SQLException("Failed to create token");
        }
    }
    public void updateToken(String tokenId, String refreshToken, Timestamp expiresAt) throws SQLException {
        String sql = """
                    UPDATE user_tokens 
                    SET refresh_token = ?, 
                        expires_at = ?, 
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, refreshToken);
            stmt.setTimestamp(2, expiresAt);
            stmt.setObject(3, UUID.fromString(tokenId));  // Convert string to UUID
            int updatedRows = stmt.executeUpdate();
            if (updatedRows == 0) {
                throw new SQLException("Token update failed, no rows affected.");
            }
        }
    }
    public void deleteTokensByUserId(String userId) throws SQLException {
        String sql = "DELETE FROM user_tokens WHERE user_id = ?";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(userId));
            stmt.executeUpdate();
        }
    }
    private UserToken mapResultSetToToken(ResultSet rs) throws SQLException {
        return new UserToken(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("refresh_token"),
                rs.getTimestamp("expires_at"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
    }
}