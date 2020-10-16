
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.sql.SQLException;


public class Bot extends TelegramLongPollingBot {
    private static final String botUserName = "ArthurNotes_bot";
    private static final String botToken = "1184832674:AAHF7L6XsIRLEi604MYbetZuVyXOzUgWMzk";

    private static Bot instance;
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);

    public static Bot getBot(){
        if(instance == null) {
            instance = new Bot();
        }
        return instance;
    }

    public void onUpdateReceived(Update update) {
        if(!update.hasMessage()) return;
        Message message = update.getMessage();
        if(!message.hasText()) return;
        String messageText = update.getMessage().getText();
        try {
            GuestInteractor.receiveMessage(message.getFrom().getId(),messageText, message.getChatId());
        } catch (SQLException e) {
            logger.info("Exception: ", e.toString());
        }
    }

    public synchronized void sendMsg(String chatId, String s) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
           logger.info("Exception: ", e.toString());
        }
    }

    public String getBotUsername() {
        return botUserName;
    }

    public String getBotToken() {
        return botToken;
    }
}
