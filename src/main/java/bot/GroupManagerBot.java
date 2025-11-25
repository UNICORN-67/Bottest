package bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
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

    // IMPORTANT: Replace with your actual Bot Username and Token
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
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Chat chat = message.getChat();

            // --- NEW CHECK: Only process updates from groups or supergroups ---
            if (!chat.isGroupChat() && !chat.isSuperGroupChat()) {
                // Optionally, you can send a message explaining the bot is for groups
                /* sendMessage(message.getChatId(), "👋 I am a Group Management Bot and only work in groups!"); */
                return; 
            }
            // --- END NEW CHECK ---

            // Handle New Members
            if (message.getNewChatMembers() != null) {
                handleNewMembers(message);
            }

            // Handle Text Commands
            if (message.hasText()) {
                handleCommand(message);
            }
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
        String command = parts[0].toLowerCase();

        // Admin check is skipped for /start and /help, but applied to moderation commands
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
            sendMessage(chatId, "❌ An error occurred (Check Bot's Admin Rights): " + e.getMessage());
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

        // 1. Ban (Kick = Ban then Unban immediately)
        BanChatMember banMethod = new BanChatMember();
        banMethod.setChatId(chatId);
        banMethod.setUserId(userIdToKick);
        execute(banMethod);
        
        // 2. Unban immediately to allow re-joining
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
        // Mute for 1 hour 
        restrict.setUntilDate((int) (Instant.now().getEpochSecond() + 3600)); 

        execute(restrict);
        sendMessage(chatId, "🔇 " + userName + " has been muted for 1 hour.");
    }

    private void unmuteUser(Message message) throws TelegramApiException {
        if (!isReply(message)) {
            sendMessage(message.getChatId(), "Reply to a user to unmute them.");
            return;
        }

        Long chatId = message.getChatId();
        Long userIdToUnmute = message.getReplyToMessage().getFrom().getId();
        String userName = message.getReplyToMessage().getFrom().getFirstName();

        // Restore all default permissions (Unmute)
        ChatPermissions permissions = new ChatPermissions();
        permissions.setCanSendMessages(true);
        permissions.setCanSendMediaMessages(true);
        permissions.setCanSendOtherMessages(true);
        permissions.setCanAddWebPagePreviews(true);
        permissions.setCanChangeInfo(true);
        permissions.setCanInviteUsers(true);
        permissions.setCanPinMessages(true);


        RestrictChatMember restrict = new RestrictChatMember();
        restrict.setChatId(chatId);
        restrict.setUserId(userIdToUnmute);
        restrict.setPermissions(permissions);

        execute(restrict);
        sendMessage(chatId, "🔊 " + userName + " has been unmuted.");
    }

    private void sendHelp(Long chatId) {
        String help = "🤖 **Group Manager Bot Help**\n\n" +
                      "*Note: Commands only work in groups!*\n\n" +
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
        sm.setParseMode("Markdown"); // Use Markdown for better formatting
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            // This is where you might see errors like "Forbidden: bot is not an administrator"
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
