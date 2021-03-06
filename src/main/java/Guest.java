import java.util.Objects;

public class Guest {
    private int id;
    private BotCommands currentCommand;
    private long chatId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Guest guest = (Guest) o;
        return id == guest.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public int getId() {
        return id;
    }

    public Guest(int id, long chatId) {
        this.id = id;
        this.chatId = chatId;
        currentCommand = BotCommands.NONE;
    }

    public BotCommands getCurrentCommand(){
        return currentCommand;
    }

    public void setCurrentCommand(BotCommands currentCommand){
        this.currentCommand = currentCommand;
    }

    public void setChatId(long chatId){
        this.chatId = chatId;
    }

    public long getChatId(){
        return chatId;
    }
}
