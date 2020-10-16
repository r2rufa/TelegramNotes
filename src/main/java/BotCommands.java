public enum BotCommands {
    START,
    ADD,
    DELETE,
    LIST,
    NONE;

    public static BotCommands getCommand(String string){
        BotCommands botCommand = NONE;
        switch (string){
            case "/start": botCommand = START;
            break;
            case "/add": botCommand = ADD;
            break;
            case "/delete": botCommand = DELETE;
            break;
            case "/list": botCommand = LIST;
            break;
        }
        return botCommand;
    }
}
