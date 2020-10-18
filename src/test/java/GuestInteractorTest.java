import org.junit.Rule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.mockito.Matchers.*;
import static org.powermock.api.mockito.PowerMockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest(GuestInteractor.class)
@PowerMockIgnore({"org.mockito.*"})

class GuestInteractorTest<PowerMockRule> {
   @Rule
   public org.powermock.modules.junit4.rule.PowerMockRule rule = new org.powermock.modules.junit4.rule.PowerMockRule();
    private static String messageWrongCommand = "/hello";
    private static String messageAddCommand = "/add";
    private static String messageDeleteCommand = "/delete";
    private static String messageListCommand = "/list";
    private static String messageStartCommand = "/start";
    private static String noteIdMessage = "2";
    private static String noteMessage = "Hello world";
    private static Guest guest;
    private static long chatId = 123123L;
    private static int guestId = 123;




    @BeforeAll
    static void beforeAll() {
        guest = new Guest(guestId, chatId);

    }

    @Test
    void receiveMessage() {
        //не смог пока разобраться как воспользоваться данными классами для моего случая: метод статический, ничего не возвращает
        //надо в результате вызова метода посчитать количество вызовов методов bot.sendMsg, processCommand и processMessage в зависимости
        //от входящего сообщения. В оставшихся двух методах аналогично.
        PowerMockito.mockStatic(GuestInteractor.class);
        doNothing().when(GuestInteractor.class);
        GuestInteractor.receiveMessage(anyInt(), anyString(), anyLong());
        verifyStatic(GuestInteractor.class);
        GuestInteractor.receiveMessage(anyInt(), anyString(), anyLong());
    }

    @Test
    void processCommand() {
    }

    @Test
    void processMessage() {
    }

}