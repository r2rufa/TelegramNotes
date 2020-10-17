
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class GuestInteractor {
    private static Bot bot = Bot.getBot();
    private static Map<Integer,Guest> guestMap= new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(GuestInteractor.class);

    public static void receiveMessage(int guestId, String messageText, long chatId) {
        Guest guest;
        if(guestMap.containsKey(guestId)){
            guest = guestMap.get(guestId);
        } else {
            guest = new Guest(guestId, chatId);
            guestMap.put(guestId, guest);
        }
        if(messageText.startsWith("/")){
            BotCommands botCommand = BotCommands.getCommand(messageText);
            if(botCommand == BotCommands.NONE){
                bot.sendMsg(chatId, "неверная команда, повторите команду.");
            } else {
                processCommand(botCommand, guest);
            }
        } else {
            processMessage(messageText, guest);
        }
    }

    public static void processCommand(BotCommands botCommand, Guest guest) {
        long chatId = guest.getChatId();
        switch (botCommand){
            case START: bot.sendMsg(chatId,"этот бот сохраняет ваши заметки.");
            sendDefaultMessage(chatId);
            break;
            case ADD: bot.sendMsg(chatId,"напишите текст заметки");
            guest.setCurrentCommand(botCommand);
            break;
            case DELETE: bot.sendMsg(chatId,"напишите номер заметки, которую нужно удалить");
            guest.setCurrentCommand(botCommand);
            break;
            case LIST: sendNotesList(guest);
            sendDefaultMessage(chatId);
            guest.setCurrentCommand(BotCommands.NONE);
            break;
        }
    }

    public static void processMessage(String messageText, Guest guest) {
        int guestId = guest.getId();
        long chatId = guest.getChatId();
        BotCommands currentCommand = guest.getCurrentCommand();
        if(currentCommand == BotCommands.ADD){
            addNote(messageText, guestId, chatId);
        } else if(currentCommand == BotCommands.DELETE){
            boolean resetCurrentCommand = deleteNote(messageText, guestId, chatId);
            if (!resetCurrentCommand) return;
        }
        sendDefaultMessage(chatId);
        guest.setCurrentCommand(BotCommands.NONE);
    }

    static void sendDatabaseFailNotification(long chatId){
        bot.sendMsg(chatId, "не удалось подключиться к базе данных.");
    }

    public static void sendDefaultMessage(long chatId){
        bot.sendMsg(chatId, "Для управления заметками используйте следующие команды:\n " +
                "/add - для добавления заметки \n " +
                "/delete - для удаления заметки \n " +
                "/list - для получения всех заметок.");
    }

    public static void sendNotesList(Guest guest) {
        StringBuilder result = new StringBuilder();
        long chatId = guest.getChatId();
        try (Connection connection = SQLConnect.getConnection()){
            PreparedStatement statementNotesList = connection.prepareStatement(
                    "SELECT note, noteid FROM guests_notes WHERE userid = ?");
            statementNotesList.setString(1, String.valueOf(guest.getId()));
            ResultSet resultSet = statementNotesList.executeQuery();
            while (resultSet.next()){
                String note = resultSet.getString("note");
                String noteId = resultSet.getString("noteid");
                result.append(String.format("(%s) %s \n", noteId, note));
            }
            result = new StringBuilder((result.length() == 0) ? "заметки отсутствуют!" : result.toString());
        } catch (DatabaseConnectionException | SQLException e){
            sendDatabaseFailNotification(chatId);
            if(e instanceof SQLException){
                logger.info("Couldn't execute query:" + e.toString());
            }
        }
        bot.sendMsg(chatId, result.toString());
    }

    static void addNote(String messageText, int guestId, long chatId){
        //добавляем запись в базу, если успешно - сообщаем что запись добавлена
        try (Connection connection = SQLConnect.getConnection()){
            int lastNoteId = 1;
            PreparedStatement statementLastNoteId = connection.prepareStatement(
                    "SELECT max(noteId) FROM guests_notes WHERE userid = ? GROUP BY userid");
            statementLastNoteId.setString(1, String.valueOf(guestId));
            ResultSet resultSet = statementLastNoteId.executeQuery();
            if (resultSet.next()){
                lastNoteId = resultSet.getInt("max(noteId)");
                lastNoteId++;
            }
            PreparedStatement statementInsertNote = connection.prepareStatement(
                    "INSERT INTO guests_notes VALUES(?, ?, ?);");
            statementInsertNote.setString(1, String.valueOf(guestId));
            statementInsertNote.setString(2, messageText);
            statementInsertNote.setString(3, String.valueOf(lastNoteId));
            statementInsertNote.execute();
            bot.sendMsg(chatId, "заметка сохранена.");
        } catch (SQLException | DatabaseConnectionException e) {
            sendDatabaseFailNotification(chatId);
            if(e instanceof SQLException){
                logger.info("Couldn't execute query:" + e.toString());
            }
        }
    }

    static boolean deleteNote(String messageText, int guestId, long chatId){
        //парсим в сообщении число int, ищем сообщение, если нашли удаляем, пишем что удалено успешно,
        // иначе выводим сообщение что заметка не найдена
        try(Connection connection = SQLConnect.getConnection()){
            int noteId = Integer.parseInt(messageText);
            PreparedStatement statementGetRemovableNote = connection.prepareStatement(
                    "SELECT note FROM guests_notes WHERE userid = ? AND noteid = ?");
            statementGetRemovableNote.setString(1, String.valueOf(guestId));
            statementGetRemovableNote.setString(2, String.valueOf(noteId));
            ResultSet resultSet = statementGetRemovableNote.executeQuery();
            if(!resultSet.next()){
                bot.sendMsg(chatId, "не найдена заметка под данным номером, попробуйте снова.");
                return false;
            }
            connection.setAutoCommit(false);
            try {
                PreparedStatement statementDeletingNote = connection.prepareStatement(
                        "DELETE FROM guests_notes WHERE userid = ? AND noteid = ?");
                statementDeletingNote.setString(1, String.valueOf(guestId));
                statementDeletingNote.setString(2, String.valueOf(noteId));
                statementDeletingNote.execute();
                PreparedStatement statementDecrementNotesId = connection.prepareStatement(
                        "UPDATE guests_notes SET noteid = noteid - 1 WHERE userid = ? AND noteid > ?");
                statementDecrementNotesId.setString(1, String.valueOf(guestId));
                statementDecrementNotesId.setString(2, String.valueOf(noteId));
                statementDecrementNotesId.execute();
                connection.commit();
            } catch (SQLException e){
                connection.rollback();
                logger.info("Couldn't execute queue of queries:" + e.toString());
                sendDatabaseFailNotification(chatId);
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
            bot.sendMsg(chatId, "заметка удалена.");
        } catch (NumberFormatException | SQLException | DatabaseConnectionException e){
            if(e instanceof NumberFormatException) {
                bot.sendMsg(chatId, "введите номер заметки, которую хотите удалить. Сообщение должно содержать только цифры.");
                return false;
            } else if(e instanceof SQLException){
                logger.info("Couldn't execute query:" + e.toString());
            }
            sendDatabaseFailNotification(chatId);
        }
        return true;
    }
}
