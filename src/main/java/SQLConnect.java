import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class SQLConnect {
    private static final String url = "jdbc:mysql://localhost:3306/telegrambotguests?useUnicode=true&serverTimezone=UTC";
    private static final String user = "root";
    private static final String password = "root";
    private static Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(SQLConnect.class);

    public static Connection getConnection() throws DatabaseConnectionException {
        try {
            if(connection==null||connection.isClosed()){
                connect();
            }
        } catch (SQLException e) {
            logger.info("Error checking if connection is closed: " + e.toString());
            throw new DatabaseConnectionException();
        }
        return connection;
    }

    public static void connect() throws DatabaseConnectionException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.info("Error with MySQL JDBC Driver: "+ e.toString());
            throw new DatabaseConnectionException();
        }
        try{
            Connection conn = DriverManager.getConnection(url,user,password);
            System.out.println("SQL is connected!");
            connection = conn;
        } catch (SQLException e) {
            logger.info("DriverManager couldn't connect: " + e.toString());
            throw new DatabaseConnectionException();
        }
    }

}
