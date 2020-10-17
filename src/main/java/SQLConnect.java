import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class SQLConnect implements AutoCloseable {
    private static final String url = "jdbc:mysql://localhost:3306/telegrambotguests?useUnicode=true&serverTimezone=UTC";
    private static final String user = "root";
    private static final String password = "root";
    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(SQLConnect.class);

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    public SQLConnect() throws DatabaseConnectionException {
        connect();
    }

    public Connection getConnection() throws DatabaseConnectionException {
        try {
            if(connection==null||connection.isClosed()){
                connect();
            }
        } catch (SQLException e) {
            logger.info("Error checking of connection is closed: " + e.toString());
            throw new DatabaseConnectionException();
        }
        return connection;
    }

    public void connect() throws DatabaseConnectionException {
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

    public boolean noteExists(int guestId, int noteId) throws SQLException {
        PreparedStatement statementGetRemovableNote = connection.prepareStatement(
                "SELECT note FROM guests_notes WHERE userid = ? AND noteid = ?");
        statementGetRemovableNote.setString(1, String.valueOf(guestId));
        statementGetRemovableNote.setString(2, String.valueOf(noteId));
        ResultSet resultSet = statementGetRemovableNote.executeQuery();
        return resultSet.next();
    }

    public void deleteNote(int guestId, int noteId) throws SQLException {
        PreparedStatement statementDeletingNote = connection.prepareStatement(
                "DELETE FROM guests_notes WHERE userid = ? AND noteid = ?");
        statementDeletingNote.setString(1, String.valueOf(guestId));
        statementDeletingNote.setString(2, String.valueOf(noteId));
        statementDeletingNote.execute();
    }

    public void decrementNotesId(int guestId, int noteId) throws SQLException {
        PreparedStatement statementDecrementNotesId = connection.prepareStatement(
                "UPDATE guests_notes SET noteid = noteid - 1 WHERE userid = ? AND noteid > ?");
        statementDecrementNotesId.setString(1, String.valueOf(guestId));
        statementDecrementNotesId.setString(2, String.valueOf(noteId));
        statementDecrementNotesId.execute();
    }

    public int getNewNoteId(int guestId) throws SQLException {
        int noteId = 1;
        PreparedStatement statementLastNoteId = connection.prepareStatement(
                "SELECT max(noteId) FROM guests_notes WHERE userid = ? GROUP BY userid");
        statementLastNoteId.setString(1, String.valueOf(guestId));
        ResultSet resultSet = statementLastNoteId.executeQuery();
        if (resultSet.next()){
            noteId += resultSet.getInt("max(noteId)");
        }
        return noteId;
    }

    public void addNote(int guestId, int noteId, String note) throws SQLException {
        PreparedStatement statementInsertNote = connection.prepareStatement(
                "INSERT INTO guests_notes VALUES(?, ?, ?);");
        statementInsertNote.setString(1, String.valueOf(guestId));
        statementInsertNote.setString(2, note);
        statementInsertNote.setString(3, String.valueOf(noteId));
        statementInsertNote.execute();
    }

    public List<String> getNotesList(int guestId) throws SQLException {
        List<String> notesList = new ArrayList<>();
        StringBuilder nextString = new StringBuilder();
        PreparedStatement statementNotesList = connection.prepareStatement(
                "SELECT note, noteid FROM guests_notes WHERE userid = ?");
        statementNotesList.setString(1, String.valueOf(guestId));
        ResultSet resultSet = statementNotesList.executeQuery();
        while (resultSet.next()){
            String note = resultSet.getString("note");
            String noteId = resultSet.getString("noteid");
            String subString = (String.format("(%s) %s \n", noteId, note));
            if(nextString.length() + subString.length() > 1000){
                notesList.add(nextString.toString());
                nextString = new StringBuilder(subString);
            } else {
                nextString.append(subString);
            }
        }
        if(nextString.length()>0){
            notesList.add(nextString.toString());
        }
        if(notesList.size() == 0){
            notesList.add("заметки отсутствуют!");
        }
        return notesList;
    }
}
