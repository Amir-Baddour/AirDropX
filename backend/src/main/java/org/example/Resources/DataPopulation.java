package org.example.Resources;
import org.example.Infra.JdbcConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
public class DataPopulation {
    private static final Logger logger = LoggerFactory.getLogger(DataPopulation.class.getName());
    public void init() {
        logger.info("Starting database migration process...");
        try (Connection connection = JdbcConnection.connect()) {
            logger.info("Database connection established successfully");
            populateRoles(connection);
        } catch (SQLException e) {
            logger.error("Migration failed: {}", e.getMessage(), e);
        }
    }
    public static void populateRoles(Connection connection) throws SQLException {
        logger.info("Starting to populate roles table...");
        String[] roles = {"USER", "ADMIN", "SUPERADMIN"};
        String checkSql = "SELECT name FROM roles WHERE name = ?";
        String insertSql = "INSERT INTO roles (name) VALUES (?)";
        try (var checkStmt = connection.prepareStatement(checkSql);
             var insertStmt = connection.prepareStatement(insertSql)) {
            for (String role : roles) {
                checkStmt.setString(1, role);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    insertStmt.setString(1, role);
                    insertStmt.executeUpdate();
                    logger.info("Created role: {}", role);
                } else {
                    logger.info("Role already exists: {}", role);
                }
            }
            logger.info("Roles population completed successfully");
        } catch (SQLException e) {
            logger.error("Error populating roles: {}", e.getMessage());
            throw e;
        }
    }
}
