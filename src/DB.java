import java.sql.*;

public final class DB {
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            System.err.println("MySQL Connector/J not found. Put mysql-connector-j.jar inside lib/");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASSWORD);
    }
}
