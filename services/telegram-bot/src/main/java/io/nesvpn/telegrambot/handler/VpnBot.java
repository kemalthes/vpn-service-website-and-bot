package io.nesvpn.telegrambot.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class VpnBot extends TelegramLongPollingBot {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

    private final MessageHandler messageHandler;
    private final CallbackQueryHandler callbackQueryHandler;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasChannelPost()) {
            messageHandler.handleChannelPost(update.getChannelPost());
        } else if (update.hasMessage() && update.getMessage().hasDice()) {
            messageHandler.handleDice(update.getMessage());
        } else if (update.hasMessage()) {
            messageHandler.handle(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            callbackQueryHandler.handle(update.getCallbackQuery());
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
