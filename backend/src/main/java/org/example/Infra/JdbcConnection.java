package org.example.Infra;
import org.example.Config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class JdbcConnection {
    private static final String url = Config.getDbURL();
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }
}
