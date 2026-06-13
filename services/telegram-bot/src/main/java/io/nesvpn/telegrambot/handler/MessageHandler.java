package io.nesvpn.telegrambot.handler;

import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.handler.common.TelegramMessageSender;
import io.nesvpn.telegrambot.handler.sections.BalancePaymentHandler;
import io.nesvpn.telegrambot.handler.sections.BroadcastHandler;
import io.nesvpn.telegrambot.handler.sections.Lucky777Handler;
import io.nesvpn.telegrambot.handler.sections.StartMenuHandler;
import io.nesvpn.telegrambot.handler.sections.SubscriptionHandler;
import io.nesvpn.telegrambot.model.TelegramUser;
import io.nesvpn.telegrambot.services.BroadcastService;
import io.nesvpn.telegrambot.services.TelegramUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@RequiredArgsConstructor
public class MessageHandler {

    private final TelegramUserService telegramUserService;
    private final BroadcastService broadcastService;
    private final BalancePaymentHandler balancePaymentHandler;
    private final StartMenuHandler startMenuHandler;
    private final SubscriptionHandler subscriptionHandler;
    private final Lucky777Handler lucky777Handler;
    private final BroadcastHandler broadcastHandler;
    private final TelegramMessageSender sender;

    public void handle(Message message) {
        String text = message.getText();
        if (message.getFrom() == null) {
            return;
        }

        Long fromId = message.getFrom().getId();
        TelegramUser currentTelegramUser = telegramUserService.findOrCreate(fromId);
        if (isNavigationCommand(text)) {
            sender.removeReplyKeyboard(message.getChatId());
        }

        if (text != null && text.startsWith("/start")) {
            startMenuHandler.handleStart(message);
        } else if ("/profile".equals(text)) {
            startMenuHandler.handleProfile(message);
        } else if ("/referrals".equals(text)) {
            startMenuHandler.handleReferrals(message);
        } else if ("/instructions".equals(text)) {
            startMenuHandler.handleInstructions(message);
        } else if ("/balance".equals(text)) {
            balancePaymentHandler.handleBalance(message);
        } else if ("/subscriptions".equals(text)) {
            subscriptionHandler.handleSubscription(message);
        } else if ("/info".equals(text)) {
            startMenuHandler.handleAboutService(message);
        } else {
            BotState state = BotState.fromString(currentTelegramUser.getState());

            if (state == BotState.BROADCAST_AWAITING_POST && broadcastService.isAdmin(fromId)) {
                broadcastHandler.handleAdminBroadcastPost(message);
            } else if (text == null) {
                sender.deleteMessage(message.getChatId(), message.getMessageId());
            } else if (lucky777Handler.isLucky777BackText(state, text)) {
                lucky777Handler.handleBackText(message);
            } else if (lucky777Handler.isLucky777SpinText(state, text)) {
                lucky777Handler.handleSpinText(message.getChatId());
            } else if (lucky777Handler.isLucky777State(state)) {
                lucky777Handler.handleInvalidMessage(message.getChatId());
            } else if (state == BotState.BALANCE_AWAITING_AMOUNT) {
                balancePaymentHandler.handleAmountInput(message);
            } else if (state == BotState.BALANCE_AWAITING_AMOUNT_CRYPTO) {
                balancePaymentHandler.handleAmountInputCrypto(message);
            } else {
                sender.deleteMessage(message.getChatId(), message.getMessageId());
            }
        }
    }

    public void handleDice(Message message) {
        lucky777Handler.handleDice(message);
    }

    private boolean isNavigationCommand(String text) {
        return text != null && (text.startsWith("/start")
                || text.equals("/profile")
                || text.equals("/referrals")
                || text.equals("/instructions")
                || text.equals("/balance")
                || text.equals("/subscriptions")
                || text.equals("/info"));
    }
}
