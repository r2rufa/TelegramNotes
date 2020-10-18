import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SQLConnectTest {
    private static SQLConnect sqlConnect;
    private static int guestId = 123;
    private static long chatId = 123412341234L;
    private static Map<Integer, String> notes;

    @BeforeAll
    static void beforeAll() throws DatabaseConnectionException, SQLException {
        sqlConnect = new SQLConnect();
        clearAllNotes(guestId);
        notes = new HashMap<>(); //size must me not less than 2
        notes.put(1, "Note 1");
        notes.put(2, "Note 2");
        notes.put(3, "Note 3");
        addAllNotes();
    }

    @AfterAll
    static void afterAll() throws DatabaseConnectionException, SQLException {
        clearAllNotes(guestId);
    }

    @Test
    void close() throws SQLException, DatabaseConnectionException {
        Connection connection = sqlConnect.getConnection();
        sqlConnect.close();
        assertEquals(connection.isClosed(), true);
    }

    @Test
    void getConnection() throws SQLException, DatabaseConnectionException {
        Connection connection = sqlConnect.getConnection();
        assertEquals(connection.isClosed(), false);
    }

    @Test
    void getConnectionWhenClosed() throws SQLException, DatabaseConnectionException {
        Connection connection = sqlConnect.getConnection();
        sqlConnect.close();
        Connection connectionNew = sqlConnect.getConnection();
        assertNotEquals(connection, connectionNew);
    }

    @Test
    void noteExists() throws SQLException {
        assertEquals(sqlConnect.noteExists(guestId, notes.size()+1), false);
        for(Map.Entry<Integer, String> pair: notes.entrySet()){
            assertTrue(sqlConnect.noteExists(guestId, pair.getKey()));
        }
    }

    @Test
    void deleteNote() throws SQLException, DatabaseConnectionException {
        if(notes.size()<2){
            System.out.println("too short map of test notes");
            return;
        }
        int removableNoteId = notes.size()-1;
        int numberOfNotesBefore = getNumberOfNotes(guestId);
        assertTrue(sqlConnect.noteExists(guestId,removableNoteId));
        sqlConnect.deleteNote(guestId, removableNoteId);
        int numberOfNotesAfter = getNumberOfNotes(guestId);
        assertEquals(numberOfNotesBefore, numberOfNotesAfter-1);
        assertFalse(sqlConnect.noteExists(guestId, removableNoteId));
        clearAllNotes(guestId);
        addAllNotes();
    }

    @Test
    void decrementNotesId() throws SQLException, DatabaseConnectionException {
        if(notes.size()<2){
            System.out.println("too short map of test notes");
            return;
        }
        int emptyNoteId = notes.size()-1;
        sqlConnect.deleteNote(guestId, emptyNoteId);
        Map<Integer, String> lastNoteBefore = getNote(guestId);
        Map.Entry<Integer,String> entry = lastNoteBefore.entrySet().iterator().next();
        int lastNoteIdBefore = entry.getKey();
        String lastNoteStringBefore = entry.getValue();
        Map<Integer, String> nextNote = getNote(guestId, emptyNoteId+1);
        String nextNoteString = nextNote.get(emptyNoteId+1);

        sqlConnect.decrementNotesId(guestId, emptyNoteId);

        Map<Integer, String> lastNoteAfter = getNote(guestId);
        Map<Integer, String> currentNote = getNote(guestId, emptyNoteId);
        assertEquals(lastNoteAfter, lastNoteBefore);
        //assertEquals(l);
    }

    @Test
    void getNewNoteId() {
    }

    @Test
    void addNote() {
    }

    @Test
    void getNotesList() {
    }

    private static void clearAllNotes(int id) throws SQLException, DatabaseConnectionException {
        Connection connection = sqlConnect.getConnection();
        PreparedStatement statementDeletingNote = connection.prepareStatement(
                "DELETE FROM guests_notes WHERE userid = ?");
        statementDeletingNote.setString(1, String.valueOf(id));
        statementDeletingNote.execute();
    }

    private static void addAllNotes() throws SQLException {
        for(Map.Entry<Integer, String> pair: notes.entrySet()){
            sqlConnect.addNote(guestId, pair.getKey(), pair.getValue());
        }
    }

    private static int getNumberOfNotes(int id) throws DatabaseConnectionException, SQLException {
        Connection connection = sqlConnect.getConnection();
        PreparedStatement statementLastNoteId = connection.prepareStatement(
                "SELECT COUNT(noteId) FROM guests_notes WHERE userid = ?");
        statementLastNoteId.setString(1, String.valueOf(id));
        ResultSet resultSet = statementLastNoteId.executeQuery();
        if (resultSet.next()){
            return resultSet.getInt("COUNT(noteId)");
        } else return 0;
    }

    private static Map<Integer, String> getNote(int guestId) throws SQLException, DatabaseConnectionException {
        Map<Integer, String> result = new HashMap<>();
        Connection connection = sqlConnect.getConnection();
        PreparedStatement statementLastNoteId = connection.prepareStatement(
                "SELECT MAX(noteId) FROM guests_notes WHERE userid = ?");
        statementLastNoteId.setString(1, String.valueOf(guestId));
        ResultSet resultSet = statementLastNoteId.executeQuery();
        int lastNoteId;
        if (resultSet.next()){
            lastNoteId =  resultSet.getInt("MAX(noteId)");
        } else return result;
        PreparedStatement statementLastNote = connection.prepareStatement(
                "SELECT note FROM guests_notes WHERE userid = ? AND noteId = ?");
        statementLastNote.setString(1, String.valueOf(guestId));
        statementLastNote.setString(2, String.valueOf(lastNoteId));
        ResultSet resultSet2 = statementLastNote.executeQuery();
        String note;
        if (resultSet2.next()){
            note = resultSet.getString("note");
            result.put(lastNoteId, note);
        }
        return result;
    }

    private static Map<Integer, String> getNote(int guestId, int noteId) throws SQLException, DatabaseConnectionException {
        Map<Integer, String> result = new HashMap<>();
        Connection connection = sqlConnect.getConnection();
        PreparedStatement statementNote = connection.prepareStatement(
                "SELECT note FROM guests_notes WHERE userid = ? AND noteId = ?");
        statementNote.setString(1, String.valueOf(guestId));
        statementNote.setString(2, String.valueOf(noteId));
        ResultSet resultSet = statementNote.executeQuery();
        String note;
        if (resultSet.next()){
            note = resultSet.getString("note");
            result.put(noteId, note);
        }
        return result;
    }
}