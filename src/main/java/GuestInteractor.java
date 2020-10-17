
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
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
            case LIST: sendingNotesList(guest);
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
            addingNote(messageText, guestId, chatId);
        } else if(currentCommand == BotCommands.DELETE){
            int noteId = getNoteIdFromMessage(messageText);
            if(noteId == 0) {
                sendWrongNumberMessage(chatId);
                return;
            }
            boolean resetCurrentCommand = deletingNote(noteId, guestId, chatId);
            if (!resetCurrentCommand) return;
        }
        sendDefaultMessage(chatId);
        guest.setCurrentCommand(BotCommands.NONE);
    }

    private static void sendDatabaseFailNotification(long chatId){
        bot.sendMsg(chatId, "не удалось подключиться к базе данных.");
    }

    private static void sendDefaultMessage(long chatId){
        bot.sendMsg(chatId, "Для управления заметками используйте следующие команды:\n " +
                "/add - для добавления заметки \n " +
                "/delete - для удаления заметки \n " +
                "/list - для получения всех заметок.");
    }

    private static void sendingNotesList(Guest guest) {
        long chatId = guest.getChatId();
        try (SQLConnect sqlConnect = new SQLConnect()){
            List<String> notesList = sqlConnect.getNotesList(guest.getId());
            for(String message: notesList){
                bot.sendMsg(chatId, message);
            }
        } catch (DatabaseConnectionException | SQLException e){
            sendDatabaseFailNotification(chatId);
            if(e instanceof SQLException){
                logger.info("Couldn't execute query:" + e.toString());
            }
        }

    }

    private static void addingNote(String messageText, int guestId, long chatId){
        //добавляем запись в базу, если успешно - сообщаем что запись добавлена
        try (SQLConnect sqlConnect = new SQLConnect()){
            int noteId = sqlConnect.getNewNoteId(guestId);
            sqlConnect.addNote(guestId, noteId, messageText);
            bot.sendMsg(chatId, "заметка сохранена.");
        } catch (SQLException | DatabaseConnectionException e) {
            sendDatabaseFailNotification(chatId);
            if(e instanceof SQLException){
                logger.info("Couldn't execute query:" + e.toString());
            }
        }
    }

    private static boolean deletingNote(int noteId, int guestId, long chatId){
        //парсим в сообщении число int, ищем сообщение, если нашли удаляем, пишем что удалено успешно,
        // иначе выводим сообщение что заметка не найдена
        try(SQLConnect sqlConnect = new SQLConnect()){
            if(!sqlConnect.noteExists(guestId, noteId)){
                bot.sendMsg(chatId, "не найдена заметка под данным номером, попробуйте снова.");
                return false;
            }
            Connection connection = sqlConnect.getConnection();
            connection.setAutoCommit(false);
            try {
                sqlConnect.deleteNote(guestId, noteId);
                sqlConnect.decrementNotesId(guestId, noteId);
                connection.commit();
            } catch (SQLException e){
                connection.rollback();
                logger.info("Couldn't execute one of queries in transaction:" + e.toString());
                sendDatabaseFailNotification(chatId);
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
            bot.sendMsg(chatId, "заметка удалена.");
        } catch (SQLException | DatabaseConnectionException e){
            if(e instanceof SQLException){
                logger.info("Couldn't execute query:" + e.toString());
            }
            sendDatabaseFailNotification(chatId);
        }
        return true;
    }

    private static int getNoteIdFromMessage(String messageText){
        try{
            return Integer.parseInt(messageText);
        } catch (NumberFormatException e){
            return 0;
        }
    }

    private static void sendWrongNumberMessage(long chatId){
        bot.sendMsg(chatId, "введите номер заметки, которую хотите удалить. Сообщение должно содержать только цифры.");
    }


}
