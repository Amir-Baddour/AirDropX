package org.example.Infra.Persistence.Task;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.Core.Task.Model.AirdropTask;
import org.example.Core.Task.Model.TaskType;
import org.example.Infra.JdbcConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskRepository {
    private static final String COLUMNS = "id, airdrop_id, type, title, description, config::text AS config, position";

    public AirdropTask create(String airdropId, TaskType type, String title, String description, JsonObject config) throws SQLException {
        String sql = "INSERT INTO airdrop_tasks (airdrop_id, type, title, description, config, position) " +
                "VALUES (?, ?, ?, ?, ?::jsonb, " +
                "(SELECT COALESCE(MAX(position), 0) + 1 FROM airdrop_tasks WHERE airdrop_id = ?)) " +
                "RETURNING " + COLUMNS;
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            UUID id = UUID.fromString(airdropId);
            stmt.setObject(1, id);
            stmt.setString(2, type.name());
            stmt.setString(3, title);
            stmt.setString(4, description);
            stmt.setString(5, config.toString());
            stmt.setObject(6, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
            throw new SQLException("Failed to create task");
        }
    }

    public List<AirdropTask> listByAirdrop(String airdropId) throws SQLException {
        try (Connection conn = JdbcConnection.connect()) {
            return listByAirdrop(conn, airdropId);
        }
    }

    public List<AirdropTask> listByAirdrop(Connection conn, String airdropId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM airdrop_tasks WHERE airdrop_id = ? ORDER BY position";
        List<AirdropTask> result = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(airdropId));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public boolean delete(String airdropId, String taskId) throws SQLException {
        String sql = "DELETE FROM airdrop_tasks WHERE id = ? AND airdrop_id = ?";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.fromString(taskId));
            stmt.setObject(2, UUID.fromString(airdropId));
            return stmt.executeUpdate() == 1;
        }
    }

    private AirdropTask map(ResultSet rs) throws SQLException {
        return new AirdropTask(
                rs.getString("id"),
                rs.getString("airdrop_id"),
                TaskType.valueOf(rs.getString("type")),
                rs.getString("title"),
                rs.getString("description"),
                JsonParser.parseString(rs.getString("config")).getAsJsonObject(),
                rs.getInt("position")
        );
    }
}
