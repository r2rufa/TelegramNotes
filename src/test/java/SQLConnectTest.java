import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SQLConnectTest {
    private static SQLConnect sqlConnect;
    private static int guestId = 123;
    private static Map<Integer, String> notes;

    @BeforeAll
    static void beforeAll() throws DatabaseConnectionException, SQLException {
        sqlConnect = new SQLConnect();
        notes = new HashMap<>(); //size must me not less than 2
        notes.put(1, "Note 1");
        notes.put(2, "Note 2");
        notes.put(3, "Note 3");
    }

    @AfterAll
    static void afterAll() throws DatabaseConnectionException, SQLException {
        clearAllNotes(guestId);
        sqlConnect.close();
    }

    @BeforeEach
    void setUp() throws SQLException, DatabaseConnectionException {
        clearAllNotes(guestId);
        addAllNotes();
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
            assertEquals(sqlConnect.noteExists(guestId, pair.getKey()), true);
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
        assertEquals(sqlConnect.noteExists(guestId,removableNoteId), true);
        sqlConnect.deleteNote(guestId, removableNoteId);
        int numberOfNotesAfter = getNumberOfNotes(guestId);
        assertEquals(numberOfNotesBefore, numberOfNotesAfter+1);
        assertEquals(sqlConnect.noteExists(guestId, removableNoteId), false);
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
        int lastNoteIdBefore = (int)lastNoteBefore.keySet().toArray()[0];
        String lastNoteStringBefore = lastNoteBefore.get(lastNoteIdBefore);

        Map<Integer, String> nextNote = getNote(guestId, emptyNoteId+1);
        String nextNoteStringBefore = nextNote.get(emptyNoteId+1);

        sqlConnect.decrementNotesId(guestId, emptyNoteId);

        Map<Integer, String> lastNoteAfter = getNote(guestId);
        int lastNoteIdAfter = (int)lastNoteAfter.keySet().toArray()[0];
        String lastNoteStringAfter = lastNoteAfter.get(lastNoteIdAfter);

        Map<Integer, String> currentNote = getNote(guestId, emptyNoteId);
        String currentNoteStringAfter = currentNote.get(emptyNoteId);

        assertEquals(lastNoteStringAfter, lastNoteStringBefore);
        assertEquals(lastNoteIdBefore, lastNoteIdAfter + 1);
        assertEquals(nextNoteStringBefore, currentNoteStringAfter);
    }

    @Test
    void getNewNoteId() throws SQLException {
        assertEquals(sqlConnect.getNewNoteId(guestId), notes.size()+1);
    }

    @Test
    void addNote() throws SQLException, DatabaseConnectionException {
        String newNote = "Extra note";
        int newNoteId = sqlConnect.getNewNoteId(guestId);
        sqlConnect.addNote(guestId, newNoteId, newNote);

        Map<Integer, String> lastNoteAfter = getNote(guestId);
        int lastNoteIdAfter = (int)lastNoteAfter.keySet().toArray()[0];
        String lastNoteStringAfter = lastNoteAfter.get(lastNoteIdAfter);

        assertEquals(lastNoteIdAfter, newNoteId);
        assertEquals(lastNoteStringAfter, newNote);
    }

    @Test
    void getNotesList() {
        StringBuilder expectedResult = new StringBuilder();
        for(int i = 1; i<= notes.size();i++){
            expectedResult.append(String.format("(%s) %s \n", i, notes.get(i)));
        }
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
        // возвращает последнюю запись переданного пользователя
        Map<Integer, String> result = new HashMap<>();
        Connection connection = sqlConnect.getConnection();

        PreparedStatement statementLastNote = connection.prepareStatement(
                "SELECT note, noteId FROM guests_notes WHERE userid = ? AND noteId = (SELECT MAX(noteId) FROM guests_notes WHERE userid = ?)");
        statementLastNote.setString(1, String.valueOf(guestId));
        statementLastNote.setString(2, String.valueOf(guestId));
        ResultSet resultSet = statementLastNote.executeQuery();
        String note;
        int lastNoteId;
        if (resultSet.next()){
            note = resultSet.getString("note");
            lastNoteId = resultSet.getInt("noteId");
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