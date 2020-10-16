
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class GuestInteractor {
    private static Bot bot = Bot.getBot();
    private static Map<Integer,Guest> guestMap= new HashMap<Integer,Guest>();

    public static void receiveMessage(int guestId, String message, long chatId) throws SQLException {
        Guest guest = null;
        if(guestMap.containsKey(guestId)){
            guest = guestMap.get(guestId);
        } else {
            guest = new Guest(guestId);
            guestMap.put(guestId, guest);
            guest.setChatId(chatId);
        }
        boolean isCommand = false;
        if(message.startsWith("/")){
            BotCommands botCommand = BotCommands.getCommand(message);
            if(botCommand == null){
                bot.sendMsg(String.valueOf(chatId), "неверная команда, повторите команду.");
            } else {
                processCommand(botCommand, guest);
            }
        } else {
            processMessage(message, guest);
        }
    }

    public static void processCommand(BotCommands botCommand, Guest guest) throws SQLException {
        String messageText = "";
        switch (botCommand){
            case START: bot.sendMsg(String.valueOf(guest.getChatId()),"этот бот сохраняет ваши заметки.");
            sendDefaultMessage(guest);
            break;
            case ADD: bot.sendMsg(String.valueOf(guest.getChatId()),"напишите текст заметки");
            guest.setCurrentCommand(botCommand);
            break;
            case DELETE: bot.sendMsg(String.valueOf(guest.getChatId()),"напишите номер заметки, которую нужно удалить");
            guest.setCurrentCommand(botCommand);
            break;
            case LIST: sendNotesList(guest);
            sendDefaultMessage(guest);
            guest.setCurrentCommand(null);
            break;
        }
    }

    public static void sendNotesList(Guest guest) {
        String result = "";
        try (Connection connection = SQLConnect.getConnection();){
            PreparedStatement statement = connection.prepareStatement("select note, noteid from guests_notes where userid = ?");
            statement.setString(1, String.valueOf(guest.getId()));
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                String note = resultSet.getString("note");
                String noteId = resultSet.getString("noteid");
                result+= String.format("(%s) %s \n", noteId, note);
            }
            result = (result.isEmpty())? "заметки отсутствуют!": result;
        } catch (SQLException e){
            result = "не удалось подключиться к базе данных. Попробуйте позже";
        }
        bot.sendMsg(String.valueOf(guest.getChatId()), result);
    }

    public static void sendDefaultMessage(Guest guest){
        bot.sendMsg(String.valueOf(guest.getChatId()),"Для управления заметками используйте следующие команды:\n /add - для добавления заметки \n /delete - для удаления заметки \n /list - для получения всех заметок.");
    }

    public static void processMessage(String messageText, Guest guest) {
        String result = "";
        BotCommands currentCommand = guest.getCurrentCommand();
        if(currentCommand == null) sendDefaultMessage(guest);
        else if(currentCommand == BotCommands.ADD){
            //добавляем запись в базу, если успешно - сообщаем что запись добавлена
            try (Connection connection = SQLConnect.getConnection()){
                int lastNoteId = 1;
                PreparedStatement statement = connection.prepareStatement("select max(noteId) from guests_notes where userid = ? group by userid");
                statement.setString(1, String.valueOf(guest.getId()));
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()){
                    lastNoteId = resultSet.getInt("max(noteId)");
                    lastNoteId++;
                }
                PreparedStatement statement1 = connection.prepareStatement("insert into guests_notes values(?, ?, ?);");
                statement1.setString(1, String.valueOf(guest.getId()));
                statement1.setString(2, messageText);
                statement1.setString(3, String.valueOf(lastNoteId));
                statement1.execute();
            } catch (SQLException e) {
                result = "не удалось подключиться к базе данных.";
            }
            bot.sendMsg(String.valueOf(guest.getChatId()), "заметка сохранена.");
            sendDefaultMessage(guest);
            guest.setCurrentCommand(null);
        } else if(currentCommand == BotCommands.DELETE){
            //парсим в сообщении число int, ищем сообщение, если нашли удаляем, пишем что удалено успешно, иначе выводим сообщение что заметка не найдена
            try(Connection connection = SQLConnect.getConnection()){
                int noteId = Integer.parseInt(messageText);
                PreparedStatement statement = connection.prepareStatement("select note from guests_notes where userid = ? and noteid = ?");
                statement.setString(1, String.valueOf(guest.getId()));
                statement.setString(2, String.valueOf(noteId));
                ResultSet resultSet = statement.executeQuery();
                if(!resultSet.next()){
                    bot.sendMsg(String.valueOf(guest.getChatId()), "не найдена заметка под данным номером, попробуйте снова.");
                    return;
                }
                PreparedStatement statement1 = connection.prepareStatement("delete from guests_notes where userid = ? and noteid = ?");
                statement1.setString(1, String.valueOf(guest.getId()));
                statement1.setString(2, String.valueOf(noteId));
                statement1.execute();
                PreparedStatement statement2 = connection.prepareStatement("update guests_notes set noteid = noteid - 1 where userid = ? and noteid > ?");
                statement2.setString(1, String.valueOf(guest.getId()));
                statement2.setString(2, String.valueOf(noteId));
                statement2.execute();
                bot.sendMsg(String.valueOf(guest.getChatId()), "заметка удалена.");
                sendDefaultMessage(guest);
                guest.setCurrentCommand(null);
            } catch (NumberFormatException | SQLException e){
                if(e instanceof SQLException){
                    result = "не удалось подключиться к базе данных.";
                    sendDefaultMessage(guest);
                    guest.setCurrentCommand(null);
                } else if(e instanceof NumberFormatException){
                    bot.sendMsg(String.valueOf(guest.getChatId()), "введите номер заметки, которую хотите удалить. Сообщение должно содержать только цифры.");
                }
            }
        }
    }

}
