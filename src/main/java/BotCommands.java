public enum BotCommands {
    START,
    ADD,
    DELETE,
    LIST;

    public static BotCommands getCommand(String string){
        BotCommands botCommand = null;
        switch (string){
            case "/start": botCommand = BotCommands.START;
            break;
            case "/add": botCommand = BotCommands.ADD;
            break;
            case "/delete": botCommand = BotCommands.DELETE;
            break;
            case "/list": botCommand = BotCommands.LIST;
            break;
        }
        return botCommand;
    }
}
