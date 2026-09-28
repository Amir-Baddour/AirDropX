package org.example.Infra.Persistence.Airdrop;

import org.example.Core.Airdrop.Model.AirdropEvent;
import org.example.Infra.JdbcConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AirdropEventRepository {
    public void add(Connection conn, String airdropId, String type, String message) throws SQLException {
        String sql = "INSERT INTO airdrop_events (airdrop_id, type, message) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            stmt.setString(2, type);
            stmt.setString(3, message);
            stmt.executeUpdate();
        }
    }

    public void add(String airdropId, String type, String message) throws SQLException {
        try (Connection conn = JdbcConnection.connect()) {
            add(conn, airdropId, type, message);
        }
    }

    public List<AirdropEvent> listByAirdrop(String airdropId, int limit) throws SQLException {
        String sql = "SELECT id, airdrop_id, type, message, created_at FROM airdrop_events " +
                "WHERE airdrop_id = ? ORDER BY id DESC LIMIT ?";
        List<AirdropEvent> result = new ArrayList<>();
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new AirdropEvent(
                            rs.getLong("id"),
                            rs.getString("airdrop_id"),
                            rs.getString("type"),
                            rs.getString("message"),
                            AirdropRepository.iso(rs.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return result;
    }
}
