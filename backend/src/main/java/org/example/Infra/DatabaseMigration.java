package org.example.Infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);

    public void init() {
        logger.info("Starting database migration process...");
        try (Connection connection = JdbcConnection.connect()) {
            logger.info("Database connection established successfully");
//          reset();
            createUuidExtension(connection);
            createRolesTable(connection);
            createUsersTable(connection);
            createUserTokensTable(connection);
            createNetworksTable(connection);
            createNetworkInitialSupply(connection);
            createNetworkInstructionsTable(connection);
            logger.info("Table creation sequence completed successfully");
        } catch (SQLException e) {
            logger.error("Migration failed: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    public void reset() {
        logger.info("Starting database reset process...");
        try (Connection connection = JdbcConnection.connect()) {
            logger.info("Database connection established successfully");
            dropTables(connection);
            logger.info("Database reset completed successfully");
        } catch (SQLException e) {
            logger.error("Reset failed: {}", e.getMessage(), e);
        }
    }
    private static void dropTables(Connection connection) throws SQLException {
        logger.info("Dropping existing tables...");
        try (Statement stmt = connection.createStatement()) {
            String[] dropStatements = {
                    "DROP TYPE IF EXISTS network_instructions CASCADE",
                    "DROP TABLE IF EXISTS network_initial_supply CASCADE",
                    "DROP TABLE IF EXISTS networks CASCADE",
                    "DROP TABLE IF EXISTS user_tokens CASCADE",
                    "DROP TABLE IF EXISTS users CASCADE",
                    "DROP TABLE IF EXISTS roles CASCADE",
                    "DROP TYPE IF EXISTS vm_type CASCADE",
            };
            for (String sql : dropStatements) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    logger.error("Error executing: " + sql + " - " + e.getMessage());
                    if (sql.contains("networks")) {
                        try {
                            stmt.execute("DROP SCHEMA public CASCADE");
                            stmt.execute("CREATE SCHEMA public");
                            stmt.execute("GRANT ALL ON SCHEMA public TO current_user");
                            stmt.execute("GRANT ALL ON SCHEMA public TO public");
                            logger.info("Schema reset successful");
                            return;
                        } catch (SQLException schemaEx) {
                            logger.error("Schema reset failed: " + schemaEx.getMessage());
                        }
                    }
                }
            }
        }
        logger.info("All tables dropped successfully.");
    }
    private static void createRolesTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS roles (" +
                "id SERIAL PRIMARY KEY," +
                "name varchar(100) NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    private static void createUsersTable(Connection connection) throws SQLException {
        String tableName = "users";
        boolean exists = tableExists(connection, tableName);
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "provider VARCHAR(255) NOT NULL DEFAULT 'GOOGLE', " +
                "provider_id VARCHAR(255) NOT NULL, " +
                "pfp VARCHAR(255), " +
                "address VARCHAR(255), " +
                "role_id INTEGER NOT NULL, " +
                "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)" +
                ")";
        String triggerFunction = "CREATE OR REPLACE FUNCTION update_updated_at_column() " +
                "RETURNS TRIGGER AS $$ " +
                "BEGIN " +
                "    NEW.updated_at = CURRENT_TIMESTAMP; " +
                "    RETURN NEW; " +
                "END; " +
                "$$ language 'plpgsql'";
        String trigger = "CREATE OR REPLACE TRIGGER update_users_updated_at " +
                "    BEFORE UPDATE ON users " +
                "    FOR EACH ROW " +
                "    EXECUTE FUNCTION update_updated_at_column()";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            if (!exists) {
                logger.info("Creating trigger function and trigger for table {}", tableName);
                stmt.execute(triggerFunction);
                stmt.execute(trigger);
            }
        }
    }
    private static void createUserTokensTable(Connection connection) throws SQLException {
        String tableName = "user_tokens";
        boolean exists = tableExists(connection, tableName);
        String sql = "CREATE TABLE IF NOT EXISTS user_tokens (" +
                "id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), " +
                "user_id UUID NOT NULL, " +
                "refresh_token TEXT NOT NULL, " +
                "expires_at TIMESTAMP WITH TIME ZONE NOT NULL, " +
                "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_user_token_user FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";
        String triggerFunction = "CREATE OR REPLACE FUNCTION update_updated_at_column() " +
                "RETURNS TRIGGER AS $$ " +
                "BEGIN " +
                "    NEW.updated_at = CURRENT_TIMESTAMP; " +
                "    RETURN NEW; " +
                "END; " +
                "$$ language 'plpgsql'";
        String trigger = "CREATE OR REPLACE TRIGGER update_user_tokens_updated_at " +
                "    BEFORE UPDATE ON user_tokens " +
                "    FOR EACH ROW " +
                "    EXECUTE FUNCTION update_updated_at_column()";
        String createIndexes = "CREATE INDEX IF NOT EXISTS idx_user_tokens_user_id ON user_tokens(user_id)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(createIndexes);
            if (!exists) {
                logger.info("Creating trigger function and trigger for table {}", tableName);
                stmt.execute(triggerFunction);
                stmt.execute(trigger);
            }
        }
    }
    private static void createNetworksTable(Connection connection) throws SQLException {
        String tableName = "networks";
        boolean exists = tableExists(connection, tableName);
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = connection.getMetaData().getTypeInfo();
            boolean enumExists = false;
            while (rs.next()) {
                if (rs.getString("TYPE_NAME").equals("vm_type")) {
                    enumExists = true;
                    break;
                }
            }

            if (!enumExists) {
                stmt.execute("CREATE TYPE vm_type AS ENUM ('EVM', 'SOLANA')");
            }
        }
        String sql = "CREATE TABLE IF NOT EXISTS networks (" +
                "id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), " +
                "user_id UUID NOT NULL, " +
                "name VARCHAR(100) UNIQUE NOT NULL, " +
                "rpc_url VARCHAR(255) UNIQUE NOT NULL, " +
                "ip_address VARCHAR(40) UNIQUE NOT NULL, " +
                "dns VARCHAR(255) UNIQUE NOT NULL, " +
                "network_image TEXT, " +
                "chain_id VARCHAR(50) UNIQUE, " +
                "vm_type vm_type NOT NULL, " +
                "gas_price BIGINT, " +
                "node_wrapper_address VARCHAR(255), " +
                "foundation_address VARCHAR(255), " +
                "gas_price_address VARCHAR(255), " +
                "automation_revenue_address VARCHAR(255), " +
                "currency_name VARCHAR(50) NOT NULL UNIQUE, " +
                "currency_symbol VARCHAR(50) NOT NULL, " +
                "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_network_user FOREIGN KEY (user_id) REFERENCES users(id) " +
                ")";
        String createIndexes =
                "CREATE INDEX IF NOT EXISTS idx_networks_user_id ON networks(user_id); ";
        String triggerFunction = "CREATE OR REPLACE FUNCTION update_updated_at_column() " +
                "RETURNS TRIGGER AS $$ " +
                "BEGIN " +
                "    NEW.updated_at = CURRENT_TIMESTAMP; " +
                "    RETURN NEW; " +
                "END; " +
                "$$ language 'plpgsql'";
        String trigger = "CREATE OR REPLACE TRIGGER update_networks_updated_at " +
                "    BEFORE UPDATE ON networks " +
                "    FOR EACH ROW " +
                "    EXECUTE FUNCTION update_updated_at_column()";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(createIndexes);
            if (!exists) {
                stmt.execute(triggerFunction);
                stmt.execute(trigger);
            }
        }
    }
    private static void createNetworkInitialSupply(Connection connection) throws SQLException {
        String tableName = "network_initial_supply";
        boolean exists = tableExists(connection, tableName);
        String sql = "CREATE TABLE IF NOT EXISTS network_initial_supply (" +
                "id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), " +
                "network_id UUID NOT NULL, " +
                "address VARCHAR(255) NOT NULL, " +
                "amount VARCHAR(255) NOT NULL, " +
                "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_network_initial_supply_network FOREIGN KEY (network_id) REFERENCES networks(id)" +
                ")";
        String createIndexes =
                "CREATE INDEX IF NOT EXISTS idx_network_initial_supply_network_id ON network_initial_supply(network_id); " +
                        "CREATE INDEX IF NOT EXISTS idx_network_initial_supply_address ON network_initial_supply(address)";
        String triggerFunction = "CREATE OR REPLACE FUNCTION update_updated_at_column() " +
                "RETURNS TRIGGER AS $$ " +
                "BEGIN " +
                "    NEW.updated_at = CURRENT_TIMESTAMP; " +
                "    RETURN NEW; " +
                "END; " +
                "$$ language 'plpgsql'";
        String trigger = "CREATE OR REPLACE TRIGGER update_network_initial_supply_updated_at " +
                "    BEFORE UPDATE ON network_initial_supply " +
                "    FOR EACH ROW " +
                "    EXECUTE FUNCTION update_updated_at_column()";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(createIndexes);
            if (!exists) {
                logger.info("Creating trigger function and trigger for table {}", tableName);
                stmt.execute(triggerFunction);
                stmt.execute(trigger);
            }
        }
    }
    private static void createNetworkInstructionsTable(Connection connection) throws SQLException {
        String tableName = "network_instructions";
        boolean exists = tableExists(connection, tableName);
        String sql = "CREATE TABLE IF NOT EXISTS network_instructions (" +
                "network_name VARCHAR(100) PRIMARY KEY, " +
                "first_command TEXT NOT NULL, " +
                "second_command TEXT NOT NULL, " +
                "FOREIGN KEY(network_name) REFERENCES networks(name)" +
                ");";
        if (!exists) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
        }
    }
    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName.toLowerCase(), null)) {
            return rs.next();
        }
    }
    private static void createUuidExtension(Connection connection) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTypeInfo()) {
            boolean hasUUID = false;
            while (rs.next()) {
                if ("uuid".equalsIgnoreCase(rs.getString("TYPE_NAME"))) {
                    hasUUID = true;
                    break;
                }
            }
            String sql = "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
                logger.info(hasUUID ?
                        "UUID extension already exists" :
                        "UUID extension created successfully");
            }
        }
    }
}