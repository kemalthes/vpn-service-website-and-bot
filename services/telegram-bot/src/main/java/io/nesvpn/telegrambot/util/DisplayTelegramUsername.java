package io.nesvpn.telegrambot.util;

import io.nesvpn.telegrambot.handler.VpnBot;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramBot;

@Slf4j
public class DisplayTelegramUsername {

    private DisplayTelegramUsername() {
    }

    public static String getDisplayName(VpnBot bot, Long chatId) {
        try {
            Chat chat = bot.execute(new GetChat(chatId.toString()));

            return chat.getUserName() != null
                    ? "@" + chat.getUserName()
                    : chat.getFirstName() != null
                    ? chat.getFirstName()
                    : "Пользователь";

        } catch (TelegramApiException e) {
            log.warn("Не удалось получить данные пользователя {}: {}", chatId, e.getMessage());
            return "Пользователь";
        }
    }
}
