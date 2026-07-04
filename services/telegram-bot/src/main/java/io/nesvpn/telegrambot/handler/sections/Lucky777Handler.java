package io.nesvpn.telegrambot.handler.sections;

import io.nesvpn.telegrambot.dto.lucky777.Lucky777Result;
import io.nesvpn.telegrambot.dto.lucky777.Lucky777Status;
import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.handler.common.TelegramDeliveryStatus;
import io.nesvpn.telegrambot.handler.common.TelegramMessageSender;
import io.nesvpn.telegrambot.model.TelegramUser;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.Lucky777Service;
import io.nesvpn.telegrambot.services.TelegramUserService;
import io.nesvpn.telegrambot.services.TokenService;
import io.nesvpn.telegrambot.services.UserService;
import io.nesvpn.telegrambot.util.KeyboardFactory;
import io.nesvpn.telegrambot.util.TextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class Lucky777Handler {

    private final UserService userService;
    private final TelegramUserService telegramUserService;
    private final TokenService tokenService;
    private final Lucky777Service lucky777Service;
    private final SubscriptionHandler subscriptionHandler;
    private final TextFactory textFactory;
    private final KeyboardFactory keyboardFactory;
    private final TelegramMessageSender sender;

    public void handleDice(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        User user = userService.findOrCreateByTgId(userId);
        TelegramUser telegramUser = telegramUserService.findOrCreate(userId);
        BotState state = BotState.fromString(telegramUser.getState());

        if (!isLucky777State(state)) {
            return;
        }

        Token token = tokenService.getUserToken(user.getId());
        if (token == null) {
            sender.sendMessage(chatId, textFactory.lucky777NoTokenText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        if (message.getDice() == null || !lucky777Service.isSlotDice(message.getDice().getEmoji())) {
            sender.sendMessage(chatId, textFactory.lucky777InvalidDiceText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        if (isForwardedMessage(message)) {
            sender.sendMessage(chatId, textFactory.lucky777ForwardedDiceText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        try {
            Lucky777Result result = lucky777Service.processDice(user, message.getDice().getValue());
            telegramUserService.updateState(userId, BotState.SUBSCRIPTION_LUCKY_777, BotState.SUBSCRIPTIONS);

            if (result.noToken()) {
                sender.sendMessage(chatId, textFactory.lucky777NoTokenText(), keyboardFactory.getBackButton(), "HTML");
            } else if (!result.processed()) {
                sender.sendMessage(
                        chatId,
                        textFactory.lucky777CooldownText(formatRemaining(result.remaining())),
                        keyboardFactory.getBackButton(),
                        "HTML"
                );
            } else {
                sender.sendMessage(
                        chatId,
                        textFactory.lucky777ResultText(result.diceValue(), result.rewardDays()),
                        keyboardFactory.getBackButton(),
                        "HTML"
                );
            }
        } catch (Exception e) {
            log.error("Lucky 777 dice handling error", e);
            sender.sendMessage(chatId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
        }
    }

    public void showLucky777(Long chatId, Integer messageId, User user) {
        Token token = tokenService.getUserToken(user.getId());
        if (token == null) {
            subscriptionHandler.showSubscription(chatId, messageId, user);
            return;
        }

        telegramUserService.updateState(user.getTgId(), BotState.SUBSCRIPTION_LUCKY_777, BotState.SUBSCRIPTIONS);
        Lucky777Status status = lucky777Service.getStatus(user);

        if (messageId != null) {
            sender.deleteMessage(chatId, messageId);
        }
        sender.sendMessage(
                chatId,
                textFactory.lucky777Text(status.canSpin(), formatRemaining(status.remaining())),
                keyboardFactory.getLucky777ReplyKeyboard(),
                "HTML"
        );
    }

    public void handleBackText(Message message) {
        sender.removeReplyKeyboard(message.getChatId());
        sender.deleteMessage(message.getChatId(), message.getMessageId());
        User user = userService.findOrCreateByTgId(message.getFrom().getId());
        subscriptionHandler.showSubscription(message.getChatId(), null, user);
    }

    public void handleSpinText(Long chatId) {
        sender.sendMessage(chatId, textFactory.lucky777KeyboardButtonText(), keyboardFactory.getBackButton(), "HTML");
    }

    public void handleInvalidMessage(Long chatId) {
        sender.sendMessage(chatId, textFactory.lucky777InvalidDiceText(), keyboardFactory.getBackButton(), "HTML");
    }

    public TelegramDeliveryStatus showLucky777AvailableNotification(Long chatId) {
        return sender.sendMessageWithStatus(
                chatId,
                textFactory.lucky777AvailableNotificationText(),
                keyboardFactory.getLucky777AvailableNotificationKeyboard(),
                "HTML"
        );
    }

    public boolean isLucky777State(BotState state) {
        return state == BotState.SUBSCRIPTION_LUCKY_777;
    }

    public boolean isLucky777BackText(BotState state, String text) {
        return isLucky777State(state) && ("Назад".equals(text) || "◀️ Назад".equals(text));
    }

    public boolean isLucky777SpinText(BotState state, String text) {
        return isLucky777State(state) && ("Прокрутить".equals(text) || "🎰".equals(text));
    }

    private boolean isForwardedMessage(Message message) {
        return message.getForwardOrigin() != null
                || message.getForwardDate() != null
                || message.getForwardFrom() != null
                || message.getForwardFromChat() != null
                || message.getForwardSenderName() != null
                || message.getForwardSignature() != null
                || Boolean.TRUE.equals(message.getIsAutomaticForward());
    }

    private String formatRemaining(Duration remaining) {
        if (remaining == null || remaining.isZero() || remaining.isNegative()) {
            return "0 минут";
        }

        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();

        if (hours > 0) {
            return String.format("%d ч %d мин", hours, minutes);
        }

        return String.format("%d мин", Math.max(1, minutes));
    }
}
