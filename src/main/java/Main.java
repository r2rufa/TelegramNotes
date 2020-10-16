import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;


public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[]args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

        try {
            telegramBotsApi.registerBot(Bot.getBot());
        } catch (TelegramApiRequestException e) {
            logger.info("Couldn't register Telegram bot: " + e.toString());
        }
    }
}
