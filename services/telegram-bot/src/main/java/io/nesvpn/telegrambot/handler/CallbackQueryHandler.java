package io.nesvpn.telegrambot.handler;

import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.handler.common.TelegramMessageSender;
import io.nesvpn.telegrambot.handler.sections.BalancePaymentHandler;
import io.nesvpn.telegrambot.handler.sections.BroadcastHandler;
import io.nesvpn.telegrambot.handler.sections.Lucky777Handler;
import io.nesvpn.telegrambot.handler.sections.StartMenuHandler;
import io.nesvpn.telegrambot.handler.sections.SubscriptionHandler;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.TelegramUserService;
import io.nesvpn.telegrambot.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.UUID;

@Slf4j
@Service
public class CallbackQueryHandler {

    private final VpnBot vpnBot;
    private final StartMenuHandler startMenuHandler;
    private final BalancePaymentHandler balancePaymentHandler;
    private final SubscriptionHandler subscriptionHandler;
    private final Lucky777Handler lucky777Handler;
    private final BroadcastHandler broadcastHandler;
    private final UserService userService;
    private final TelegramUserService telegramUserService;
    private final TelegramMessageSender sender;

    public CallbackQueryHandler(
            @Lazy VpnBot vpnBot,
            StartMenuHandler startMenuHandler,
            BalancePaymentHandler balancePaymentHandler,
            SubscriptionHandler subscriptionHandler,
            Lucky777Handler lucky777Handler,
            BroadcastHandler broadcastHandler,
            UserService userService,
            TelegramUserService telegramUserService,
            TelegramMessageSender sender) {
        this.vpnBot = vpnBot;
        this.startMenuHandler = startMenuHandler;
        this.balancePaymentHandler = balancePaymentHandler;
        this.subscriptionHandler = subscriptionHandler;
        this.lucky777Handler = lucky777Handler;
        this.broadcastHandler = broadcastHandler;
        this.userService = userService;
        this.telegramUserService = telegramUserService;
        this.sender = sender;
    }

    public void handle(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long tgId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        answerCallback(callbackQuery.getId(), null);

        BotState currentState = BotState.fromString(telegramUserService.findOrCreate(tgId).getState());
        if (currentState == BotState.SUBSCRIPTION_LUCKY_777 && !data.equals("subscription_lucky_777")) {
            sender.removeReplyKeyboard(chatId);
        }

        User user = userService.findOrCreateByTgId(tgId);

        switch (data) {
            case "start" -> startMenuHandler.showStart(chatId, messageId, user);
            case "back" -> handleBack(chatId, messageId, user);
            case "profile" -> startMenuHandler.showProfile(chatId, messageId, user);
            case "referrals" -> startMenuHandler.showReferrals(chatId, messageId, user);
            case "instructions" -> startMenuHandler.showInstructions(chatId, messageId, user);
            case "instructions_android" -> startMenuHandler.showAndroidInstructions(chatId, messageId, user);
            case "instructions_ios" -> startMenuHandler.showIosInstructions(chatId, messageId, user);
            case "instructions_windows" -> startMenuHandler.showWindowsInstructions(chatId, messageId, user);
            case "instructions_macos" -> startMenuHandler.showMacosInstructions(chatId, messageId, user);
            case "balance" -> balancePaymentHandler.showBalance(chatId, messageId, user);
            case "balance_history" -> balancePaymentHandler.showBalanceHistory(chatId, messageId, user);
            case "balance_topup" -> balancePaymentHandler.showTopUp(chatId, messageId);
            case "payment_method_sbp" -> balancePaymentHandler.showAwaitingBalance(chatId, messageId, user);
            case "payment_method_usdt" -> balancePaymentHandler.showAwaitingBalanceWithCrypto(chatId, messageId, user);
            case "subscription" -> subscriptionHandler.showSubscription(chatId, messageId, user);
            case "subscription_devices" -> subscriptionHandler.showHwidDevices(chatId, messageId, user);
            case "subscription_extend" -> subscriptionHandler.showSubscriptionExtend(chatId, messageId, user);
            case "subscription_lucky_777" -> lucky777Handler.showLucky777(chatId, messageId, user);
            case "info" -> startMenuHandler.showAboutService(chatId, messageId);
            case "broadcast" -> broadcastHandler.showBroadcast(chatId, messageId, user);
            case "broadcast_home" -> broadcastHandler.showBroadcastHome(chatId, messageId, user);
        }

        if (data.startsWith("check_payment_sbp")) {
            String[] parts = data.replace("check_payment_sbp", "").split("_");
            String transactionId = parts[0];
            balancePaymentHandler.checkPayment(chatId, messageId, transactionId, user);
        } else if (data.startsWith("extend_confirm_")) {
            String[] parts = data.replace("extend_confirm_", "").split("_");
            Long tokenId = Long.parseLong(parts[0]);
            Long planId = Long.parseLong(parts[1]);
            subscriptionHandler.showExtendConfirm(chatId, messageId, tokenId, planId, user);
        } else if (data.startsWith("extend_process_")) {
            String[] parts = data.replace("extend_process_", "").split("_");
            Long tokenId = Long.parseLong(parts[0]);
            Long planId = Long.parseLong(parts[1]);
            subscriptionHandler.showExtendProcess(chatId, messageId, tokenId, planId, user);
        } else if (data.startsWith("check_payment_crypto_")) {
            String[] parts = data.replace("check_payment_crypto_", "").split("_");
            String transactionId = parts[0];
            balancePaymentHandler.checkPayment(chatId, messageId, transactionId, user);
        } else if (data.startsWith("delete_hwid_confirm_")) {
            String hwid = data.replace("delete_hwid_confirm_", "");
            subscriptionHandler.showDeleteHwidDeviceConfirm(chatId, messageId, hwid, user);
        } else if (data.startsWith("delete_hwid_confirmation_")) {
            String hwid = data.replace("delete_hwid_confirmation_", "");
            log.info("showDeleteHwidDevice CALLED");
            subscriptionHandler.showDeleteHwidDevice(chatId, messageId, hwid, user);
        } else if (data.startsWith("broadcast_refresh_")) {
            String campaignIdText = data.replace("broadcast_refresh_", "");
            try {
                Long campaignId = Long.parseLong(campaignIdText);
                broadcastHandler.refreshBroadcastProgress(chatId, messageId, user, campaignId);
            } catch (NumberFormatException e) {
                log.warn("Invalid broadcast refresh callback payload: {}", data, e);
            }
        } else if (data.startsWith("get_2_days_free_")) {
            String[] parts = data.replace("get_2_days_free_", "").split("_");
            try {
                UUID userId = UUID.fromString(parts[0]);
                subscriptionHandler.processFreeSubscription(chatId, messageId, user, userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid free subscription callback payload: {}", data, e);
            }
        }
    }

    private void handleBack(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();
        BotState previousState = telegramUserService.getPreviousState(userId);

        telegramUserService.goToPreviousState(userId);

        switch (previousState) {
            case PROFILE:
                startMenuHandler.showProfile(chatId, messageId, user);
                break;
            case REFERRALS:
                startMenuHandler.showReferrals(chatId, messageId, user);
                break;
            case SUBSCRIPTIONS:
                subscriptionHandler.showSubscription(chatId, messageId, user);
                break;
            case BALANCE:
                balancePaymentHandler.showBalance(chatId, messageId, user);
                break;
            case BALANCE_TOP_UP:
                balancePaymentHandler.showTopUp(chatId, messageId);
                break;
            case INSTRUCTIONS:
                startMenuHandler.showInstructions(chatId, messageId, user);
                break;
            case SUBSCRIPTION_HWID_DEVICES:
                subscriptionHandler.showHwidDevices(chatId, messageId, user);
                break;
            case SUBSCRIPTIONS_EXTEND:
                subscriptionHandler.showSubscriptionExtend(chatId, messageId, user);
                break;
            case SUBSCRIPTION_LUCKY_777:
                lucky777Handler.showLucky777(chatId, messageId, user);
                break;
            case START:
            default:
                startMenuHandler.showStart(chatId, messageId, user);
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
