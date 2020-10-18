import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuestTest {
    private static Guest guest;
    private static int guestId = 123456789;
    private static long chatId = 111222333444L;

    @BeforeAll
    static void beforeAll() {
        guest = new Guest(guestId, chatId);
    }

    @Test
    void getId() {
        assertEquals(guest.getId(), guestId);
    }

    @Test
    void getCurrentCommand() {
        for(BotCommands command: BotCommands.values()){
            guest.setCurrentCommand(command);
            assertEquals(guest.getCurrentCommand(), command);
        }
        guest.setCurrentCommand(BotCommands.NONE);
    }

    @Test
    void setCurrentCommand() {
        for(BotCommands command: BotCommands.values()){
            guest.setCurrentCommand(command);
            assertEquals(guest.getCurrentCommand(), command);
        }
        guest.setCurrentCommand(BotCommands.NONE);
    }

    @Test
    void setChatId() {
        long newChatId = 222333444555L;
        guest.setChatId(newChatId);
        assertEquals(guest.getChatId(), newChatId);
        guest.setChatId(chatId);
    }

    @Test
    void getChatId() {
        assertEquals(guest.getChatId(), chatId);
    }
}