package org.example.Infra.Persistence.Role;

import org.example.Core.Role.Model.Role;
import org.example.Infra.JdbcConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class RoleRepository {
    public Optional<Role> findRoleByName(String name) throws SQLException {
        String sql = "SELECT id, name FROM roles WHERE name = ?";
        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Role(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("name")
                ));
            }
        }
        return Optional.empty();
    }
    public Optional<Role> findRoleById(String id) throws SQLException {
        String sql = "SELECT id, name FROM roles WHERE id = ?";

        try (Connection conn = JdbcConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(id));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Role(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("name")
                ));
            }
        }
        return Optional.empty();
    }
}