package io.nesvpn.telegrambot.handler.sections;

import io.nesvpn.telegrambot.dto.HwidDevice;
import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.enums.SubscriptionExpirationNotificationType;
import io.nesvpn.telegrambot.handler.common.TelegramDeliveryStatus;
import io.nesvpn.telegrambot.handler.common.TelegramMessageSender;
import io.nesvpn.telegrambot.model.Order;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.services.FreeSubscriptionAwaitService;
import io.nesvpn.telegrambot.services.NotEnoughBalanceException;
import io.nesvpn.telegrambot.services.OrderService;
import io.nesvpn.telegrambot.services.SubscriptionAutoRenewalService;
import io.nesvpn.telegrambot.services.SubscriptionDevicePricingService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final SubscriptionDevicePricingService subscriptionDevicePricingService;
    private final FreeSubscriptionAwaitService freeSubscriptionAwaitService;
    private final StartMenuHandler startMenuHandler;
    private final TextFactory textFactory;
    private final KeyboardFactory keyboardFactory;
    private final TelegramMessageSender sender;

    private boolean isActiveToken(Token token) {
        return token != null && token.isActive();
    }

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
                    textFactory.subscriptionText(
                            isActive,
                            tokenUrl,
                            validTo,
                            daysLeft,
                            devicesCount,
                            subscriptionDevicePricingService.maxDevices(token),
                            token.getRenewalTargetMaxDevices(),
                            autoRenewalEnabled),
                    keyboardFactory.getSubscriptionKeyboard(token.isActive(), devicesCount, autoRenewalEnabled),
                    "HTML"
            );
        }
    }

    public void toggleAutoRenewal(Long chatId, Integer messageId, User user) {
        subscriptionAutoRenewalService.toggle(user.getId());
        showSubscription(chatId, messageId, user);
    }

    public void showSubscriptionDevicesMenu(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTION_HWID_DEVICES, BotState.SUBSCRIPTIONS);

        Token token = tokenService.getUserToken(user.getId());

        if (!isActiveToken(token)) {
            sender.editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackToSubscriptionKeyboard(), "HTML");
            return;
        }

        Integer devicesCount = null;
        try {
            List<HwidDevice> hwidDevices = tokenService.getHwidDevicesByToken(user.getId());
            if (hwidDevices != null) {
                devicesCount = hwidDevices.size();
            }
        } catch (Exception ex) {
            log.error("Failed to get HWID devices for devices menu, userId={}", user.getId(), ex);
        }

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.subscriptionDevicesMenuText(
                        devicesCount,
                        subscriptionDevicePricingService.maxDevices(token),
                        token.getRenewalTargetMaxDevices()),
                keyboardFactory.getSubscriptionDevicesMenuKeyboard(),
                "HTML"
        );
    }

    public void showHwidDevices(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTION_HWID_DEVICES, BotState.SUBSCRIPTIONS);

        Token token = tokenService.getUserToken(user.getId());

        if (!isActiveToken(token)) {
            sender.editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackToSubscriptionDevicesKeyboard(), "HTML");
            return;
        }

        List<HwidDevice> hwidDevices;
        try {
            hwidDevices = tokenService.getHwidDevicesByToken(user.getId());
        } catch (Exception ex) {
            log.error("Failed to get HWID devices for userId={}", user.getId(), ex);
            sender.editOrSendMessage(chatId, messageId, textFactory.hwidDevicesUnavailableText(), keyboardFactory.getBackToSubscriptionDevicesKeyboard(), "HTML");
            return;
        }
        if (hwidDevices == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.hwidDevicesUnavailableText(), keyboardFactory.getBackToSubscriptionDevicesKeyboard(), "HTML");
            return;
        }
        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.hwidDevicesText(hwidDevices, subscriptionDevicePricingService.maxDevices(token)),
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
            sender.editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackToSubscriptionDevicesKeyboard(), "HTML");
            return;
        }

        boolean isSuccess = tokenService.deleteHwidDeviceByToken(user.getId(), hwid);
        sender.editOrSendMessage(
                chatId,
                messageId,
                isSuccess ? textFactory.hwidDeviceDeleteSuccess() : textFactory.hwidDeviceDeleteError(),
                keyboardFactory.getSubscriptionDevicesMenuKeyboard(),
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
        int targetMaxDevices = subscriptionDevicePricingService.resolveRenewalTargetMaxDevices(token);
        Order draft = orderService.upsertRenewalDraft(user, token, targetMaxDevices);
        Map<Long, BigDecimal> planPrices = plans.stream()
                .collect(Collectors.toMap(VpnPlan::getId, plan -> subscriptionDevicePricingService.calculateRenewalPrice(plan, targetMaxDevices)));

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.extendSubscriptionText(
                        user.getBalance(),
                        validTo,
                        daysLeft,
                        subscriptionDevicePricingService.maxDevices(token),
                        targetMaxDevices),
                keyboardFactory.getExtendPlansKeyboard(draft.getId(), plans, planPrices),
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

    public TelegramDeliveryStatus showSubscriptionExpirationNotification(
            Long chatId,
            SubscriptionExpirationNotificationType type,
            String validTo
    ) {
        return sender.editOrSendMessageWithStatus(
                chatId,
                null,
                textFactory.subscriptionExpirationNotificationText(type, validTo),
                keyboardFactory.getExpiringSubscriptionMenu(),
                "HTML"
        );
    }

    public TelegramDeliveryStatus showSubscriptionAutoRenewalSuccessNotification(
            Long chatId,
            Integer planPrice,
            BigDecimal balanceAfter,
            Integer oldMaxDevices,
            Integer targetMaxDevices,
            Integer requestedMaxDevices,
            boolean deviceLimitFallback
    ) {
        return sender.sendMessageWithStatus(
                chatId,
                textFactory.subscriptionAutoRenewalSuccessText(
                        planPrice,
                        balanceAfter,
                        oldMaxDevices,
                        targetMaxDevices,
                        requestedMaxDevices,
                        deviceLimitFallback),
                keyboardFactory.getBackToSubscriptionKeyboard(),
                "HTML"
        );
    }

    public TelegramDeliveryStatus showSubscriptionAutoRenewalFailedNotification(
            Long chatId,
            SubscriptionExpirationNotificationType type,
            String validTo,
            Integer planPrice,
            BigDecimal balance
    ) {
        return sender.editOrSendMessageWithStatus(
                chatId,
                null,
                textFactory.subscriptionExpirationAutoRenewalFailedText(type, validTo, planPrice, balance),
                keyboardFactory.getExpiringSubscriptionMenu(),
                "HTML"
        );
    }

    public void showExtendConfirm(Long chatId, Integer messageId, Long orderId, Long planId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS_CONFIRM, BotState.SUBSCRIPTIONS_EXTEND);

        VpnPlan plan = vpnPlanService.findById(planId).orElse(null);
        if (plan == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        Order order;
        try {
            order = orderService.attachPlanToRenewalDraft(user, orderId, plan);
        } catch (RuntimeException ex) {
            log.warn("Failed to attach plan to renewal draft. orderId={}, planId={}", orderId, planId, ex);
            try {
                order = createRenewalDraftByTokenId(user, orderId, plan);
            } catch (RuntimeException legacyEx) {
                log.warn("Failed to create renewal draft from legacy callback. tokenId={}, planId={}", orderId, planId, legacyEx);
                sender.editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
                return;
            }
        }
        Token token = order != null && order.getTokenId() != null
                ? tokenService.findById(order.getTokenId()).orElse(null)
                : null;

        if (token == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
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
                                order.getTotalPrice(),
                                Formatter.formatMoscow(currentValidTo),
                                Formatter.formatMoscow(newValidTo),
                                user.getBalance(),
                                subscriptionDevicePricingService.maxDevices(token),
                                order.getTargetMaxDevices()
                        ),
                keyboardFactory.getConfirmExtendKeyboard(order.getId()),
                "HTML"
        );
    }

    public void showLegacyExtendProcess(Long chatId, Integer messageId, Long tokenId, Long planId, User user) {
        VpnPlan plan = vpnPlanService.findById(planId).orElse(null);
        if (plan == null) {
            sender.editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        Order order;
        try {
            order = createRenewalDraftByTokenId(user, tokenId, plan);
        } catch (RuntimeException ex) {
            log.warn("Failed to create renewal draft from legacy callback. tokenId={}, planId={}", tokenId, planId, ex);
            sender.editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        showExtendProcess(chatId, messageId, order.getId(), user);
    }

    public void showExtendProcess(Long chatId, Integer messageId, Long orderId, User user) {
        Order paidOrder;
        try {
            paidOrder = orderService.confirmDraft(user, orderId);
        } catch (NotEnoughBalanceException ex) {
            log.warn("Not enough balance to confirm renewal draft. orderId={}", orderId);
            sender.editOrSendMessage(
                    chatId,
                    messageId,
                    textFactory.notEnoughMoneyMessage(ex.getRequiredAmount(), ex.getBalance()),
                    keyboardFactory.getBackToSubscriptionKeyboard(),
                    "HTML"
            );
            return;
        } catch (RuntimeException ex) {
            log.warn("Failed to confirm renewal draft. orderId={}", orderId, ex);
            sender.editOrSendMessage(
                    chatId,
                    messageId,
                    textFactory.dataNotFoundText(),
                    keyboardFactory.getBackToSubscriptionKeyboard(),
                    "HTML"
            );
            return;
        }
        log.info("Заказ {} оплачен", paidOrder.getId());

        sender.editOrSendMessage(chatId, messageId, textFactory.successSubscribeProvidedText(), null, "HTML");
        startMenuHandler.showStart(chatId, null, user);
    }

    private Order createRenewalDraftByTokenId(User user, Long tokenId, VpnPlan plan) {
        Token token = tokenService.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found with id: " + tokenId));
        if (!user.getId().equals(token.getUserId())) {
            throw new IllegalStateException("Token user mismatch");
        }

        int targetMaxDevices = subscriptionDevicePricingService.resolveRenewalTargetMaxDevices(token);
        Order draft = orderService.upsertRenewalDraft(user, token, targetMaxDevices);
        return orderService.attachPlanToRenewalDraft(user, draft.getId(), plan);
    }

    public void showDeviceLimitInput(Long chatId, Integer messageId, User user) {
        Token token = tokenService.getUserToken(user.getId());
        if (!isActiveToken(token)) {
            sender.editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackToSubscriptionDevicesKeyboard(), "HTML");
            return;
        }

        List<VpnPlan> plans = vpnPlanService.getAllPlans();
        int standardDevices = subscriptionDevicePricingService.singleDefaultDevices(plans);
        int maxDevicesLimit = subscriptionDevicePricingService.getSettings().getMaxDevicesLimit();
        telegramUserService.updateState(user.getTgId(), BotState.SUBSCRIPTION_DEVICE_LIMIT_INPUT, BotState.SUBSCRIPTION_HWID_DEVICES);

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.deviceLimitInputText(
                        standardDevices,
                        maxDevicesLimit,
                        subscriptionDevicePricingService.maxDevices(token),
                        token.getRenewalTargetMaxDevices()),
                keyboardFactory.getBackToSubscriptionDevicesKeyboard(),
                "HTML"
        );
    }

    public void handleDeviceLimitInput(Message message) {
        Long chatId = message.getChatId();
        User user = userService.findOrCreateByTgId(message.getFrom().getId());
        Token token = tokenService.getUserToken(user.getId());
        if (!isActiveToken(token)) {
            sender.sendMessage(chatId, textFactory.tokenNotFoundText(), keyboardFactory.getBackToSubscriptionDevicesKeyboard(), "HTML");
            return;
        }

        List<VpnPlan> plans = vpnPlanService.getAllPlans();
        int standardDevices = subscriptionDevicePricingService.singleDefaultDevices(plans);
        int maxDevicesLimit = subscriptionDevicePricingService.getSettings().getMaxDevicesLimit();
        Integer targetMaxDevices = parseDevices(message.getText());
        if (targetMaxDevices == null || targetMaxDevices < standardDevices || targetMaxDevices > maxDevicesLimit) {
            sender.sendMessage(chatId, textFactory.deviceLimitInputInvalidText(standardDevices, maxDevicesLimit), keyboardFactory.getBackToSubscriptionDevicesKeyboard(), "HTML");
            return;
        }

        int currentMaxDevices = subscriptionDevicePricingService.maxDevices(token);
        if (targetMaxDevices > currentMaxDevices) {
            Order draft = orderService.upsertDeviceLimitChangeDraft(user, token, targetMaxDevices);
            telegramUserService.updateState(user.getTgId(), BotState.SUBSCRIPTIONS_CONFIRM, BotState.SUBSCRIPTION_DEVICE_LIMIT_INPUT);
            sender.sendMessage(
                    chatId,
                    textFactory.deviceLimitChangeConfirmText(
                            currentMaxDevices,
                            targetMaxDevices,
                            Formatter.formatMoscow(token.getValidTo()),
                            draft.getTotalPrice(),
                            token.getRenewalTargetMaxDevices()),
                    keyboardFactory.getConfirmDeviceLimitChangeKeyboard(draft.getId()),
                    "HTML"
            );
            return;
        }

        if (targetMaxDevices < currentMaxDevices) {
            tokenService.updateRenewalTargetMaxDevices(token.getId(), targetMaxDevices);
            telegramUserService.updateState(user.getTgId(), BotState.SUBSCRIPTION_HWID_DEVICES, BotState.SUBSCRIPTION_DEVICE_LIMIT_INPUT);
            sender.sendMessage(
                    chatId,
                    textFactory.deviceLimitDecreaseSavedText(currentMaxDevices, targetMaxDevices),
                    keyboardFactory.getSubscriptionDevicesMenuKeyboard(),
                    "HTML"
            );
            return;
        }

        boolean changed = token.getRenewalTargetMaxDevices() != null;
        tokenService.updateRenewalTargetMaxDevices(token.getId(), null);
        telegramUserService.updateState(user.getTgId(), BotState.SUBSCRIPTION_HWID_DEVICES, BotState.SUBSCRIPTION_DEVICE_LIMIT_INPUT);
        sender.sendMessage(
                chatId,
                textFactory.deviceLimitResetText(currentMaxDevices, changed),
                keyboardFactory.getSubscriptionDevicesMenuKeyboard(),
                "HTML"
        );
    }

    public void showDeviceLimitProcess(Long chatId, Integer messageId, Long orderId, User user) {
        Order paidOrder;
        try {
            paidOrder = orderService.confirmDraft(user, orderId);
        } catch (NotEnoughBalanceException ex) {
            log.warn("Not enough balance to confirm device limit draft. orderId={}", orderId);
            sender.editOrSendMessage(
                    chatId,
                    messageId,
                    textFactory.notEnoughMoneyMessage(ex.getRequiredAmount(), ex.getBalance()),
                    keyboardFactory.getBackToSubscriptionKeyboard(),
                    "HTML"
            );
            return;
        } catch (RuntimeException ex) {
            log.warn("Failed to confirm device limit draft. orderId={}", orderId, ex);
            sender.editOrSendMessage(
                    chatId,
                    messageId,
                    textFactory.dataNotFoundText(),
                    keyboardFactory.getBackToSubscriptionKeyboard(),
                    "HTML"
            );
            return;
        }
        log.info("Заказ на изменение лимита {} оплачен", paidOrder.getId());
        sender.editOrSendMessage(chatId, messageId, textFactory.successSubscribeProvidedText(), keyboardFactory.getBackToSubscriptionKeyboard(), "HTML");
    }

    private Integer parseDevices(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
