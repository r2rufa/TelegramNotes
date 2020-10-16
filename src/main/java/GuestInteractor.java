
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class GuestInteractor {
    private static Bot bot = Bot.getBot();
    private static Map<Integer,Guest> guestMap= new HashMap<>();

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
        String messageText = "";
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

    public static void sendNotesList(Guest guest) {
        StringBuilder result = new StringBuilder();
        long chatId = guest.getChatId();
        try (Connection connection = SQLConnect.getConnection()){
            PreparedStatement statement = connection.prepareStatement("select note, noteid from guests_notes where userid = ?");
            statement.setString(1, String.valueOf(guest.getId()));
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                String note = resultSet.getString("note");
                String noteId = resultSet.getString("noteid");
                result.append(String.format("(%s) %s \n", noteId, note));
            }
            result = new StringBuilder((result.length() == 0) ? "заметки отсутствуют!" : result.toString());
        } catch (DatabaseConnectionException | SQLException e){
            sendDatabaseFailNotification(chatId);
        }
        bot.sendMsg(chatId, result.toString());
    }

    public static void sendDefaultMessage(long chatId){
        bot.sendMsg(chatId,"Для управления заметками используйте следующие команды:\n /add - для добавления заметки \n /delete - для удаления заметки \n /list - для получения всех заметок.");
    }

    public static void processMessage(String messageText, Guest guest) {
        int guestId = guest.getId();
        long chatId = guest.getChatId();
        String result = "";
        BotCommands currentCommand = guest.getCurrentCommand();
        if(currentCommand == BotCommands.ADD){
            //добавляем запись в базу, если успешно - сообщаем что запись добавлена
            try (Connection connection = SQLConnect.getConnection()){
                int lastNoteId = 1;
                PreparedStatement statement = connection.prepareStatement("select max(noteId) from guests_notes where userid = ? group by userid");
                statement.setString(1, String.valueOf(guestId));
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()){
                    lastNoteId = resultSet.getInt("max(noteId)");
                    lastNoteId++;
                }
                PreparedStatement statement1 = connection.prepareStatement("insert into guests_notes values(?, ?, ?);");
                statement1.setString(1, String.valueOf(guestId));
                statement1.setString(2, messageText);
                statement1.setString(3, String.valueOf(lastNoteId));
                statement1.execute();
            } catch (SQLException | DatabaseConnectionException e) {
                sendDatabaseFailNotification(chatId);
            }
            bot.sendMsg(chatId, "заметка сохранена.");
        } else if(currentCommand == BotCommands.DELETE){
            //парсим в сообщении число int, ищем сообщение, если нашли удаляем, пишем что удалено успешно, иначе выводим сообщение что заметка не найдена
            try(Connection connection = SQLConnect.getConnection()){
                int noteId = Integer.parseInt(messageText);
                PreparedStatement statement = connection.prepareStatement("select note from guests_notes where userid = ? and noteid = ?");
                statement.setString(1, String.valueOf(guestId));
                statement.setString(2, String.valueOf(noteId));
                ResultSet resultSet = statement.executeQuery();
                if(!resultSet.next()){
                    bot.sendMsg(chatId, "не найдена заметка под данным номером, попробуйте снова.");
                    return;
                }
                PreparedStatement statement1 = connection.prepareStatement("delete from guests_notes where userid = ? and noteid = ?");
                statement1.setString(1, String.valueOf(guestId));
                statement1.setString(2, String.valueOf(noteId));
                statement1.execute();
                PreparedStatement statement2 = connection.prepareStatement("update guests_notes set noteid = noteid - 1 where userid = ? and noteid > ?");
                statement2.setString(1, String.valueOf(guestId));
                statement2.setString(2, String.valueOf(noteId));
                statement2.execute();
                bot.sendMsg(chatId, "заметка удалена.");
            } catch (NumberFormatException | SQLException | DatabaseConnectionException e){
                if(e instanceof SQLException){
                    sendDatabaseFailNotification(chatId);
                } else {
                    bot.sendMsg(chatId, "введите номер заметки, которую хотите удалить. Сообщение должно содержать только цифры.");
                    return;
                }
            }
        }
        sendDefaultMessage(chatId);
        guest.setCurrentCommand(BotCommands.NONE);
    }

    static void sendDatabaseFailNotification(long chatId){
        bot.sendMsg(chatId, "не удалось подключиться к базе данных.");
    }
}
