package bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            // Initialize the Bots API
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Register the bot
            System.out.println("Starting Group Manager Bot...");
            botsApi.registerBot(new GroupManagerBot());
            System.out.println("Bot started successfully!");
            
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
