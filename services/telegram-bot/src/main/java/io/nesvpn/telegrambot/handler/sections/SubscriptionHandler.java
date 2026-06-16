package io.nesvpn.telegrambot.handler.sections;

import io.nesvpn.telegrambot.dto.HwidDevice;
import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.enums.SubscriptionExpirationNotificationType;
import io.nesvpn.telegrambot.handler.common.TelegramMessageSender;
import io.nesvpn.telegrambot.model.Order;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.services.FreeSubscriptionAwaitService;
import io.nesvpn.telegrambot.services.OrderService;
import io.nesvpn.telegrambot.services.SubscriptionAutoRenewalService;
import io.nesvpn.telegrambot.services.TelegramUserService;
import io.nesvpn.telegrambot.services.TokenService;
import io.nesvpn.telegrambot.services.UserService;
import io.nesvpn.telegrambot.services.VpnPlanService;
import io.nesvpn.telegrambot.util.Formatter;
import io.nesvpn.telegrambot.util.KeyboardFactory;
import io.nesvpn.telegrambot.util.TextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionHandler {
    private static final long FREE_PLAN_ID = 4L;

    private final UserService userService;
    private final TelegramUserService telegramUserService;
    private final TokenService tokenService;
    private final OrderService orderService;
    private final VpnPlanService vpnPlanService;
    private final SubscriptionAutoRenewalService subscriptionAutoRenewalService;
    private final FreeSubscriptionAwaitService freeSubscriptionAwaitService;
    private final StartMenuHandler startMenuHandler;
    private final TextFactory textFactory;
    private final KeyboardFactory keyboardFactory;
    private final TelegramMessageSender sender;

    public void handleSubscription(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.SUBSCRIPTIONS);

        User user = userService.findOrCreateByTgId(userId);
        showSubscription(chatId, null, user);
    }

    public void showSubscription(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS, BotState.START);

        Token token = tokenService.getUserToken(user.getId());
        if (token == null) {
            if (!orderService.orderExists(user.getId(), FREE_PLAN_ID)) {
                sender.editOrSendMessage(
                        chatId,
                        messageId,
                        textFactory.tokenNotFoundText(),
                        keyboardFactory.getSubscriptionKeyboardFirst(user.getId()),
                        "HTML"
                );
            } else {
                sender.editOrSendMessage(
                        chatId,
                        messageId,
                        textFactory.tokenNotFoundText(),
                        keyboardFactory.getBackButton(),
                        "HTML"
                );
            }
        } else {
            long daysLeft = tokenService.getDaysLeft(token);
            boolean isActive = token.isActive();
            String tokenUrl = tokenService.getFullTokenUrl(token);
            Integer devicesCount = null;
            try {
                List<HwidDevice> hwidDevices = tokenService.getHwidDevicesByToken(user.getId());
                if (hwidDevices != null) {
                    devicesCount = hwidDevices.size();
                } else {
                    log.warn("Empty HWID devices response for subscription page, userId={}", user.getId());
                }
            } catch (Exception ex) {
                log.error("Failed to get HWID devices for subscription page, userId={}", user.getId(), ex);
            }
            String validTo = token.getValidTo() != null
                    ? Formatter.formatMoscow(token.getValidTo())
                    : "Не указано";
            boolean autoRenewalEnabled = subscriptionAutoRenewalService.isEnabled(user.getId());
            sender.editOrSendMessage(
                    chatId,
                    messageId,
                    textFactory.subscriptionText(isActive, tokenUrl, validTo, daysLeft, devicesCount, autoRenewalEnabled),
                    keyboardFactory.getSubscriptionKeyboard(token.isActive(), devicesCount, autoRenewalEnabled),
                    "HTML"
            );
        }
    }

    public void toggleAutoRenewal(Long chatId, Integer messageId, User user) {
        subscriptionAutoRenewalService.toggle(user);
        showSubscription(chatId, messageId, user);
    }

    public void showHwidDevices(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTION_HWID_DEVICES, BotState.SUBSCRIPTIONS);

        Token token = tokenService.getUserToken(user.getId());

        if (token == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        List<HwidDevice> hwidDevices;
        try {
            hwidDevices = tokenService.getHwidDevicesByToken(user.getId());
        } catch (Exception ex) {
            log.error("Failed to get HWID devices for userId={}", user.getId(), ex);
            sender.editOrSendMessage(chatId, messageId, textFactory.hwidDevicesUnavailableText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }
        if (hwidDevices == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.hwidDevicesUnavailableText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }
        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.hwidDevicesText(hwidDevices),
                keyboardFactory.getHwidDevicesKeyboard(hwidDevices),
                "HTML"
        );
    }

    public void showDeleteHwidDeviceConfirm(Long chatId, Integer messageId, String hwid, User user) {
        telegramUserService.updateState(
                user.getTgId(),
                BotState.SUBSCRIPTION_HWID_DEVICES_CONFIRM,
                BotState.SUBSCRIPTION_HWID_DEVICES
        );
        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.hwidDeviceDeleteConfirm(),
                keyboardFactory.getHwidDeviceDeleteKeyboard(hwid),
                "HTML"
        );
    }

    public void showDeleteHwidDevice(Long chatId, Integer messageId, String hwid, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS, BotState.SUBSCRIPTION_HWID_DEVICES);
        Token token = tokenService.getUserToken(user.getId());

        if (token == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        boolean isSuccess = tokenService.deleteHwidDeviceByToken(user.getId(), hwid);
        sender.editOrSendMessage(
                chatId,
                messageId,
                isSuccess ? textFactory.hwidDeviceDeleteSuccess() : textFactory.hwidDeviceDeleteError(),
                keyboardFactory.getBackButton(),
                "HTML"
        );
    }

    public void showSubscriptionExtend(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS_EXTEND, BotState.SUBSCRIPTIONS);

        Token token = tokenService.getUserToken(user.getId());

        if (token == null) {
            if (!orderService.orderExists(user.getId(), FREE_PLAN_ID)) {
                sender.editOrSendMessage(
                        chatId,
                        messageId,
                        textFactory.tokenNotFoundText(),
                        keyboardFactory.getSubscriptionKeyboardFirst(user.getId()),
                        "HTML"
                );
            } else {
                sender.editOrSendMessage(
                        chatId,
                        messageId,
                        textFactory.tokenNotFoundText(),
                        keyboardFactory.getBackButton(),
                        "HTML"
                );
            }
            return;
        }

        long daysLeft = tokenService.getDaysLeft(token);
        String validTo = token.getValidTo() != null
                ? Formatter.formatMoscow(token.getValidTo())
                : "Не указано";
        List<VpnPlan> plans = vpnPlanService.getAllPlans();

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.extendSubscriptionText(user.getBalance(), validTo, daysLeft),
                keyboardFactory.getExtendPlansKeyboard(token.getId(), plans),
                "HTML"
        );
    }

    public void processFreeSubscription(Long chatId, Integer messageId, User user, UUID callbackUserId) {
        if (!user.getId().equals(callbackUserId)) {
            log.warn("Free subscription callback user mismatch. callbackUserId={}, currentUserId={}", callbackUserId, user.getId());
            showSubscription(chatId, messageId, user);
            return;
        }
        if (orderService.orderExists(user.getId(), FREE_PLAN_ID)) {
            showSubscription(chatId, messageId, user);
            return;
        }
        VpnPlan freePlan = vpnPlanService.findById(FREE_PLAN_ID).orElse(null);
        if (freePlan == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }
        orderService.createOrder(user, freePlan);
        sender.editOrSendMessage(chatId, messageId, textFactory.successSubscribeProvidedText(), null, "HTML");
        freeSubscriptionAwaitService.waitForFreeSubscriptionAndShow(
                chatId,
                user,
                refreshedUser -> showSubscription(chatId, null, refreshedUser),
                () -> sender.sendMessage(
                        chatId,
                        "⏳ Не получилось сразу обновить подписку. Проверьте раздел «Подписка» через пару минут.",
                        keyboardFactory.getBackToSubscriptionKeyboard(),
                        "HTML"
                )
        );
    }

    public boolean showSubscriptionExpirationNotification(
            Long chatId,
            SubscriptionExpirationNotificationType type,
            String validTo
    ) {
        return sender.tryEditOrSendMessage(
                chatId,
                null,
                textFactory.subscriptionExpirationNotificationText(type, validTo),
                keyboardFactory.getExpiringSubscriptionMenu(),
                "HTML"
        );
    }

    public boolean showSubscriptionAutoRenewalSuccessNotification(
            Long chatId,
            Integer planPrice,
            BigDecimal balanceAfter
    ) {
        return sender.trySendMessage(
                chatId,
                textFactory.subscriptionAutoRenewalSuccessText(planPrice, balanceAfter),
                keyboardFactory.getBackToSubscriptionKeyboard(),
                "HTML"
        );
    }

    public boolean showSubscriptionAutoRenewalFailedNotification(
            Long chatId,
            SubscriptionExpirationNotificationType type,
            String validTo,
            Integer planPrice,
            BigDecimal balance
    ) {
        return sender.tryEditOrSendMessage(
                chatId,
                null,
                textFactory.subscriptionExpirationAutoRenewalFailedText(type, validTo, planPrice, balance),
                keyboardFactory.getExpiringSubscriptionMenu(),
                "HTML"
        );
    }

    public void showExtendConfirm(Long chatId, Integer messageId, Long tokenId, Long planId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS_CONFIRM, BotState.SUBSCRIPTIONS_EXTEND);

        Token token = tokenService.findById(tokenId).orElse(null);
        VpnPlan plan = vpnPlanService.findById(planId).orElse(null);

        if (token == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        } else if (plan == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        LocalDateTime currentValidTo = token.getValidTo() != null ? token.getValidTo() : LocalDateTime.now();
        LocalDateTime baseDate = currentValidTo.isBefore(LocalDateTime.now())
                ? LocalDateTime.now()
                : currentValidTo;
        LocalDateTime newValidTo = baseDate.plusDays(plan.getDuration());

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.extendSubscribeConfirmText(
                        plan.getName(),
                        plan.getPrice(),
                        Formatter.formatMoscow(currentValidTo),
                        Formatter.formatMoscow(newValidTo),
                        user.getBalance()
                ),
                keyboardFactory.getConfirmExtendKeyboard(tokenId, planId),
                "HTML"
        );
    }

    public void showExtendProcess(Long chatId, Integer messageId, Long tokenId, Long planId, User user) {
        Optional<Token> token = tokenService.findById(tokenId);
        VpnPlan plan = vpnPlanService.findById(planId).orElse(null);

        if (token.isEmpty() || plan == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        if (user.getBalance().compareTo(BigDecimal.valueOf(plan.getPrice())) < 0) {
            sender.editOrSendMessage(
                    chatId,
                    messageId,
                    textFactory.notEnoughMoneyMessage(plan.getPrice(), user.getBalance()),
                    keyboardFactory.getBackToSubscriptionKeyboard(),
                    "HTML"
            );
            return;
        }

        Order order = orderService.createOrder(user, plan);
        log.info("Заказ {} cоздан", order.getId());

        sender.editOrSendMessage(chatId, messageId, textFactory.successSubscribeProvidedText(), null, "HTML");
        startMenuHandler.showStart(chatId, null, user);
    }
}
