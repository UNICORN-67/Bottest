package bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.util.List;

public class GroupManagerBot extends TelegramLongPollingBot {

    // TODO: Replace with your actual Bot Username and Token
    private static final String BOT_USERNAME = "testing0_1bot";
    private static final String BOT_TOKEN = "8506848584:AAEOimtxfo2t2rA780WEqtvNR1DSxf4URvY";

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Handle New Members
        if (update.hasMessage() && update.getMessage().getNewChatMembers() != null) {
            handleNewMembers(update.getMessage());
        }

        // Handle Text Commands
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleCommand(update.getMessage());
        }
    }

    private void handleNewMembers(Message message) {
        List<User> newUsers = message.getNewChatMembers();
        for (User user : newUsers) {
            // Don't welcome the bot itself
            if (!user.getUserName().equals(getBotUsername())) {
                String welcomeText = String.format("Welcome to the group, %s! Please read the rules.", user.getFirstName());
                sendMessage(message.getChatId(), welcomeText);
            }
        }
    }

    private void handleCommand(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!text.startsWith("/")) return;

        // Split command and arguments
        String[] parts = text.split(" ", 2);
        String command = parts[0].toLowerCase(); // e.g., "/ban"

        // Only allow admins to use admin commands
        if (isAdminCommand(command) && !isAdmin(chatId, userId)) {
            sendMessage(chatId, "⚠️ You must be an admin to use this command.");
            return;
        }

        try {
            switch (command) {
                case "/start":
                case "/help":
                    sendHelp(chatId);
                    break;
                case "/ban":
                    banUser(message);
                    break;
                case "/kick":
                    kickUser(message);
                    break;
                case "/mute":
                    muteUser(message);
                    break;
                case "/unmute":
                    unmuteUser(message);
                    break;
                default:
                    // Ignore unknown commands
                    break;
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMessage(chatId, "❌ An error occurred: " + e.getMessage());
        }
    }

    // --- Command Implementations ---

    private void banUser(Message message) throws TelegramApiException {
        if (!isReply(message)) {
            sendMessage(message.getChatId(), "Reply to a user's message to ban them.");
            return;
        }

        Long chatId = message.getChatId();
        Long userIdToBan = message.getReplyToMessage().getFrom().getId();
        String userName = message.getReplyToMessage().getFrom().getFirstName();

        BanChatMember banMethod = new BanChatMember();
        banMethod.setChatId(chatId);
        banMethod.setUserId(userIdToBan);

        execute(banMethod);
        sendMessage(chatId, "🚫 " + userName + " has been banned.");
    }

    private void kickUser(Message message) throws TelegramApiException {
        if (!isReply(message)) {
            sendMessage(message.getChatId(), "Reply to a user's message to kick them.");
            return;
        }

        Long chatId = message.getChatId();
        Long userIdToKick = message.getReplyToMessage().getFrom().getId();
        String userName = message.getReplyToMessage().getFrom().getFirstName();

        // Kick logic: Ban then Unban immediately
        BanChatMember banMethod = new BanChatMember();
        banMethod.setChatId(chatId);
        banMethod.setUserId(userIdToKick);
        // Set 'until date' to current time + 1 min (approx) just to be safe, though unban works differently
        // Standard "Kick" is Ban followed by UnbanChatMember, but strictly unbanning clears the ban list.
        // For simplicity here, we just ban. To just kick, you'd unban immediately after.
        execute(banMethod);
        
        // Unban immediately to allow re-joining
        org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember unban = 
            new org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember();
        unban.setChatId(chatId);
        unban.setUserId(userIdToKick);
        execute(unban);

        sendMessage(chatId, "👢 " + userName + " has been kicked.");
    }

    private void muteUser(Message message) throws TelegramApiException {
        if (!isReply(message)) {
            sendMessage(message.getChatId(), "Reply to a user to mute them.");
            return;
        }

        Long chatId = message.getChatId();
        Long userIdToMute = message.getReplyToMessage().getFrom().getId();
        String userName = message.getReplyToMessage().getFrom().getFirstName();

        // Restrict permissions (Mute = can't send messages)
        ChatPermissions permissions = new ChatPermissions();
        permissions.setCanSendMessages(false);
        permissions.setCanSendMediaMessages(false);
        permissions.setCanSendOtherMessages(false);

        RestrictChatMember restrict = new RestrictChatMember();
        restrict.setChatId(chatId);
        restrict.setUserId(userIdToMute);
        restrict.setPermissions(permissions);
        // Mute for 1 hour for demo purposes (use setUntilDate for duration)
        restrict.setUntilDate((int) (Instant.now().getEpochSecond() + 3600)); 

        execute(restrict);
        sendMessage(chatId, "V " + userName + " has been muted for 1 hour.");
    }

    private void unmuteUser(Message message) throws TelegramApiException {
        if (!isReply(message)) {
            sendMessage(message.getChatId(), "Reply to a user to unmute them.");
            return;
        }

        Long chatId = message.getChatId();
        Long userIdToUnmute = message.getReplyToMessage().getFrom().getId();
        String userName = message.getReplyToMessage().getFrom().getFirstName();

        ChatPermissions permissions = new ChatPermissions();
        permissions.setCanSendMessages(true);
        permissions.setCanSendMediaMessages(true);
        permissions.setCanSendOtherMessages(true);

        RestrictChatMember restrict = new RestrictChatMember();
        restrict.setChatId(chatId);
        restrict.setUserId(userIdToUnmute);
        restrict.setPermissions(permissions);

        execute(restrict);
        sendMessage(chatId, "🔊 " + userName + " has been unmuted.");
    }

    private void sendHelp(Long chatId) {
        String help = "🤖 **Group Manager Bot Help**\n\n" +
                      "/ban - Ban a user (Reply to their message)\n" +
                      "/kick - Kick a user (Reply to their message)\n" +
                      "/mute - Mute a user for 1 hour (Reply to their message)\n" +
                      "/unmute - Unmute a user\n" +
                      "/help - Show this message";
        sendMessage(chatId, help);
    }

    // --- Helper Methods ---

    private void sendMessage(Long chatId, String text) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText(text);
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isReply(Message message) {
        return message.getReplyToMessage() != null;
    }

    private boolean isAdminCommand(String command) {
        return command.equals("/ban") || command.equals("/kick") || command.equals("/mute") || command.equals("/unmute");
    }

    private boolean isAdmin(Long chatId, Long userId) {
        try {
            GetChatMember getChatMember = new GetChatMember();
            getChatMember.setChatId(chatId);
            getChatMember.setUserId(userId);
            ChatMember member = execute(getChatMember);
            
            String status = member.getStatus();
            return status.equals("creator") || status.equals("administrator");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }
          }
