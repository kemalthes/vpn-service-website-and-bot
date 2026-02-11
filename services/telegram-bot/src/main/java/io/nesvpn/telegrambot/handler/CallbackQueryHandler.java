package io.nesvpn.telegrambot.handler;

import io.nesvpn.telegrambot.model.BotState;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.TelegramUserService;
import io.nesvpn.telegrambot.services.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static io.nesvpn.telegrambot.model.BotState.*;

@Service
public class CallbackQueryHandler {

    private final VpnBot vpnBot;
    private final MessageHandler messageHandler;
    private final UserService userService;
    private final TelegramUserService telegramUserService;

    public CallbackQueryHandler(
            @Lazy VpnBot vpnBot,
            MessageHandler messageHandler,
            UserService userService, TelegramUserService telegramUserService) {
        this.vpnBot = vpnBot;
        this.messageHandler = messageHandler;
        this.userService = userService;
        this.telegramUserService = telegramUserService;
    }

    public void handle(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long tgId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        answerCallback(callbackQuery.getId(), null);

        User user = userService.findOrCreateByTgId(tgId);

        switch (data) {
            case "back" -> {
                handleBack(chatId, messageId, user);
            }
            case "profile" -> {
                messageHandler.showProfile(chatId, messageId, user);
            }
            case "referrals" -> {
                messageHandler.showReferrals(chatId, messageId, user);
            }
            case "instructions" -> {
                messageHandler.showInstructions(chatId, messageId, user);
            }
            case "instructions_android" -> {
                messageHandler.showAndroidInstructions(chatId, messageId, user);
            }
            case "instructions_ios" -> {
                messageHandler.showIosInstructions(chatId, messageId, user);
            }
            case "instructions_windows" -> {
                messageHandler.showWindowsInstructions(chatId, messageId, user);
            }
            case "instructions_macos" -> {
                messageHandler.showMacosInstructions(chatId, messageId, user);
            }
            case "balance" -> {
                messageHandler.showBalance(chatId, messageId, user);
            }
            case "balance_history" -> {
                messageHandler.showBalanceHistory(chatId, messageId, user);
            }
            case "balance_topup" -> {
                messageHandler.showTopUp(chatId, messageId);
            }
            case "payment_method_sbp" -> {
                messageHandler.showAwaitingBalance(chatId, messageId, user);
            }
            case "subscription" -> {
                messageHandler.showSubscription(chatId, messageId, user);
            }
            case "subscription_extend" -> {
                messageHandler.showSubscriptionExtend(chatId, messageId, user);
            }
        }

        if (data.startsWith("check_payment_")) {
            String[] parts = data.replace("check_payment_", "").split("_");
            String transactionId = parts[0];
            Integer amount = Integer.parseInt(parts[1]);
            messageHandler.checkPayment(chatId, messageId, transactionId, amount, user);
        } else if (data.startsWith("extend_confirm_")) {
            String[] parts = data.replace("extend_confirm_", "").split("_");
            Long tokenId = Long.parseLong(parts[0]);
            Long planId = Long.parseLong(parts[1]);
            messageHandler.showExtendConfirm(chatId, messageId, tokenId, planId, user);
        } else if (data.startsWith("extend_process_")) {
            String[] parts = data.replace("extend_process_", "").split("_");
            Long tokenId = Long.parseLong(parts[0]);
            Long planId = Long.parseLong(parts[1]);
            messageHandler.showExtendProcess(chatId, messageId, tokenId, planId, user);
        }
    }

    private void handleBack(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();
        BotState previousState = telegramUserService.getPreviousState(userId);

        telegramUserService.goToPreviousState(userId);

        switch (previousState) {
            case START:
                messageHandler.showStart(chatId, messageId, user);
                break;
            case PROFILE:
                messageHandler.showProfile(chatId, messageId, user);
                break;
            case REFERRALS:
                messageHandler.showReferrals(chatId, messageId, user);
                break;
            case SUBSCRIPTIONS:
                messageHandler.showSubscription(chatId, messageId, user);
                break;
            case BALANCE:
                messageHandler.showBalance(chatId, messageId, user);
                break;
            case BALANCE_TOP_UP:
                messageHandler.showTopUp(chatId, messageId);
                break;
            case INSTRUCTIONS:
                messageHandler.showInstructions(chatId, messageId, user);
                break;
            case SUBSCRIPTIONS_EXTEND:
                messageHandler.showSubscriptionExtend(chatId, messageId, user);
                break;
            default:
                messageHandler.showStart(chatId, messageId, user);
        }
    }

    private void answerCallback(String callbackId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);

        if (text != null) {
            answer.setText(text);
            answer.setShowAlert(true);
        }

        try {
            vpnBot.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
