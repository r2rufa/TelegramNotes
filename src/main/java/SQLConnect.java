import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnect {
    private static final String url = "jdbc:mysql://localhost:3306/telegrambotguests?useUnicode=true&serverTimezone=UTC";
    private static final String user = "root";
    private static final String password = "root";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if(connection==null||connection.isClosed()){
            connect();
        }
        return connection;
    }

    public static void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try{Connection conn = DriverManager.getConnection(url,user,password);
            System.out.println("SQL is connected!");
            connection = conn;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
