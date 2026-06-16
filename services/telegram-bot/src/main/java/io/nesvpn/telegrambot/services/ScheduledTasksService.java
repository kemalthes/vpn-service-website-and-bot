package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.broadcast.BroadcastDeliveryResult;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastStats;
import io.nesvpn.telegrambot.dto.lucky777.Lucky777AvailableNotification;
import io.nesvpn.telegrambot.dto.notification.AutoRenewalResult;
import io.nesvpn.telegrambot.dto.notification.DueSubscriptionExpirationNotification;
import io.nesvpn.telegrambot.handler.sections.BalancePaymentHandler;
import io.nesvpn.telegrambot.handler.sections.BroadcastHandler;
import io.nesvpn.telegrambot.handler.sections.Lucky777Handler;
import io.nesvpn.telegrambot.handler.sections.SubscriptionHandler;
import io.nesvpn.telegrambot.model.BroadcastCampaign;
import io.nesvpn.telegrambot.model.BroadcastRecipient;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.model.SubscriptionAutoRenewalSetting;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.util.Formatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasksService {
    private static final int LUCKY_777_NOTIFICATION_BATCH_SIZE = 100;
    private static final int SUBSCRIPTION_EXPIRATION_NOTIFICATION_BATCH_SIZE = 100;
    private static final int SUBSCRIPTION_EXPIRATION_NOTIFICATION_MAX_BATCHES = 10;
    private static final int BROADCAST_BATCH_SIZE = 30;
    private static final int BROADCAST_COMPLETED_CAMPAIGNS_BATCH_SIZE = 20;
    private static final long BROADCAST_SEND_DELAY_MS = 1000L;
    private static final long BROADCAST_RECIPIENTS_CLEANUP_DELAY_MS = 86400000L;
    private static final int BROADCAST_RECIPIENTS_RETENTION_DAYS = 1;

    private final PaymentService paymentService;
    private final BalancePaymentHandler balancePaymentHandler;
    private final Lucky777Handler lucky777Handler;
    private final SubscriptionHandler subscriptionHandler;
    private final BroadcastHandler broadcastHandler;
    private final UserService userService;
    private final Lucky777Service lucky777Service;
    private final SubscriptionExpirationNotificationService subscriptionExpirationNotificationService;
    private final SubscriptionAutoRenewalService subscriptionAutoRenewalService;
    private final BroadcastService broadcastService;

    @Scheduled(fixedRate = 300000)
    public void checkExpiredPayments() {
        List<Payment> markedPayment = paymentService.markExpiredPayments();
        if (markedPayment.isEmpty()) {
            return;
        }

        List<UUID> userIds = markedPayment.stream()
                .map(Payment::getUserId)
                .distinct()
                .toList();
        Map<UUID, User> userMap = userService.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        for (Payment payment : markedPayment) {
            User user = userMap.get(payment.getUserId());
            if (user != null && user.getTgId() != null) {
                balancePaymentHandler.showExpiredPayment(user.getTgId(), payment.getTransactionToken());
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkPendingPayments() {
        List<Payment> pendingPayments = paymentService.getPendingPayments();
        if (pendingPayments.isEmpty()) {
            return;
        }
        List<Payment> confirmedPayments = new ArrayList<>();
        for (Payment payment : pendingPayments) {
            try {
                boolean confirmed = paymentService.checkAndConfirmPayment(payment);
                if (confirmed) {
                    confirmedPayments.add(payment);
                }
            } catch (Exception e) {
                log.error("Error checking payment {}", payment.getId(), e);
            }
        }
        if (confirmedPayments.isEmpty()) {
            return;
        }
        List<UUID> userIds = confirmedPayments.stream()
                .map(Payment::getUserId)
                .distinct()
                .toList();
        List<User> users = userService.getUsersByIds(userIds);
        Map<UUID, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (Payment payment : confirmedPayments) {
            User user = userMap.get(payment.getUserId());
            if (user != null) {
                balancePaymentHandler.showSuccessPayment(user.getTgId(), payment, user);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void notifyLucky777Available() {
        List<Lucky777AvailableNotification> notifications =
                lucky777Service.findAvailableNotifications(LUCKY_777_NOTIFICATION_BATCH_SIZE);
        if (notifications.isEmpty()) {
            return;
        }

        List<Lucky777AvailableNotification> sentNotifications = sendLucky777AvailableNotifications(notifications);
        lucky777Service.markAvailableNotificationsSent(sentNotifications);
    }

    private List<Lucky777AvailableNotification> sendLucky777AvailableNotifications(
            List<Lucky777AvailableNotification> notifications
    ) {
        List<UUID> userIds = notifications.stream()
                .map(Lucky777AvailableNotification::userId)
                .distinct()
                .toList();
        List<User> users = userService.getUsersByIds(userIds);
        Map<UUID, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<Lucky777AvailableNotification> sentNotifications = new ArrayList<>();

        for (Lucky777AvailableNotification notification : notifications) {
            User user = userMap.get(notification.userId());
            if (user == null || user.getTgId() == null) {
                log.warn("Lucky 777 notification skipped: user {} has no tgId", notification.userId());
                continue;
            }

            try {
                if (lucky777Handler.showLucky777AvailableNotification(user.getTgId())) {
                    sentNotifications.add(notification);
                }
            } catch (Exception e) {
                log.error("Failed to send Lucky 777 availability notification to userId={}", user.getId(), e);
            }
        }

        return sentNotifications;
    }

    @Scheduled(fixedDelay = 300000)
    public void notifySubscriptionExpiration() {
        int processed = 0;

        for (int batch = 0; batch < SUBSCRIPTION_EXPIRATION_NOTIFICATION_MAX_BATCHES; batch++) {
            List<DueSubscriptionExpirationNotification> notifications =
                    subscriptionExpirationNotificationService.findDueNotifications(SUBSCRIPTION_EXPIRATION_NOTIFICATION_BATCH_SIZE);
            if (notifications.isEmpty()) {
                return;
            }

            List<DueSubscriptionExpirationNotification> sentNotifications =
                    sendSubscriptionExpirationNotifications(notifications);
            subscriptionExpirationNotificationService.markNotificationsSent(sentNotifications);
            processed += sentNotifications.size();

            if (sentNotifications.size() < notifications.size()) {
                return;
            }

            if (notifications.size() < SUBSCRIPTION_EXPIRATION_NOTIFICATION_BATCH_SIZE) {
                return;
            }
        }

        log.warn(
                "Subscription expiration notification batch limit reached: processed={} maxBatches={} batchSize={}",
                processed,
                SUBSCRIPTION_EXPIRATION_NOTIFICATION_MAX_BATCHES,
                SUBSCRIPTION_EXPIRATION_NOTIFICATION_BATCH_SIZE
        );
    }

    private List<DueSubscriptionExpirationNotification> sendSubscriptionExpirationNotifications(
            List<DueSubscriptionExpirationNotification> notifications
    ) {
        if (notifications.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = notifications.stream()
                .map(DueSubscriptionExpirationNotification::userId)
                .distinct()
                .toList();
        Map<UUID, User> userMap = userService.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<UUID, SubscriptionAutoRenewalSetting> renewalSettingsByUserId =
                subscriptionAutoRenewalService.getSettingsByUserIds(userIds);
        boolean hasEnabledAutoRenewal = renewalSettingsByUserId.values().stream()
                .anyMatch(SubscriptionAutoRenewalSetting::isEnabled);
        VpnPlan oneMonthPlan = hasEnabledAutoRenewal
                ? subscriptionAutoRenewalService.findOneMonthPlan().orElse(null)
                : null;
        List<DueSubscriptionExpirationNotification> sentNotifications = new ArrayList<>();

        for (DueSubscriptionExpirationNotification notification : notifications) {
            User user = userMap.get(notification.userId());
            if (user == null || user.getTgId() == null) {
                log.warn(
                        "Subscription expiration notification skipped: tokenId={}, userId={} has no tgId",
                        notification.tokenId(),
                        notification.userId()
                );
                continue;
            }

            try {
                if (processSubscriptionExpirationNotification(
                        user,
                        notification,
                        renewalSettingsByUserId.get(user.getId()),
                        oneMonthPlan
                )) {
                    sentNotifications.add(notification);
                }
            } catch (Exception e) {
                log.error(
                        "Failed to send subscription expiration notification type={} tokenId={} userId={}",
                        notification.type(),
                        notification.tokenId(),
                        notification.userId(),
                        e
                );
            }
        }

        return sentNotifications;
    }

    private boolean processSubscriptionExpirationNotification(
            User user,
            DueSubscriptionExpirationNotification notification,
            SubscriptionAutoRenewalSetting renewalSetting,
            VpnPlan oneMonthPlan
    ) {
        AutoRenewalResult autoRenewalResult = subscriptionAutoRenewalService.tryRenew(user, renewalSetting, oneMonthPlan);
        String validTo = Formatter.formatMoscow(notification.validTo());

        if (autoRenewalResult.status() == AutoRenewalResult.Status.SUCCESS) {
            boolean sent = subscriptionHandler.showSubscriptionAutoRenewalSuccessNotification(
                    user.getTgId(),
                    autoRenewalResult.plan().getPrice(),
                    autoRenewalResult.balanceAfter()
            );
            if (!sent) {
                log.warn(
                        "Auto renewal succeeded but notification was not sent: tokenId={} userId={}",
                        notification.tokenId(),
                        notification.userId()
                );
            }
            return true;
        }

        if (autoRenewalResult.status() == AutoRenewalResult.Status.INSUFFICIENT_FUNDS) {
            return subscriptionHandler.showSubscriptionAutoRenewalFailedNotification(
                    user.getTgId(),
                    notification.type(),
                    validTo,
                    autoRenewalResult.plan().getPrice(),
                    autoRenewalResult.balance()
            );
        }

        if (autoRenewalResult.status() == AutoRenewalResult.Status.PLAN_NOT_FOUND) {
            log.error(
                    "Auto renewal one-month plan not found: tokenId={} userId={}",
                    notification.tokenId(),
                    notification.userId()
            );
        }

        return subscriptionHandler.showSubscriptionExpirationNotification(
                user.getTgId(),
                notification.type(),
                validTo
        );
    }

    @Scheduled(fixedDelay = 60000)
    public void processBroadcasts() {
        List<BroadcastRecipient> recipients = broadcastService.findPendingRecipients(BROADCAST_BATCH_SIZE);
        if (!recipients.isEmpty()) {
            sendBroadcastBatch(recipients);
        }

        List<BroadcastStats> completedCampaigns =
                broadcastService.completeReadyCampaigns(BROADCAST_COMPLETED_CAMPAIGNS_BATCH_SIZE);
        completedCampaigns.forEach(broadcastHandler::showBroadcastStats);
    }

    @Scheduled(fixedDelay = BROADCAST_RECIPIENTS_CLEANUP_DELAY_MS)
    public void cleanupOldBroadcastRecipients() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(BROADCAST_RECIPIENTS_RETENTION_DAYS);
        int deleted = broadcastService.deleteOldFinishedCampaignRecipients(cutoff);

        if (deleted > 0) {
            log.info("Old broadcast recipients cleaned: deleted={}, cutoff={}", deleted, cutoff);
        }
    }

    private void sendBroadcastBatch(List<BroadcastRecipient> recipients) {
        List<Long> campaignIds = recipients.stream()
                .map(BroadcastRecipient::getCampaignId)
                .distinct()
                .toList();
        Map<Long, BroadcastCampaign> campaignMap = broadcastService.getCampaignsByIds(campaignIds);
        int sentCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        log.info(
                "Broadcast batch started: recipients={}, campaigns={}, sendDelayMs={}",
                recipients.size(),
                campaignIds.size(),
                BROADCAST_SEND_DELAY_MS
        );

        for (int i = 0; i < recipients.size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Broadcast batch stopped because scheduler thread was interrupted");
                break;
            }

            BroadcastRecipient recipient = recipients.get(i);
            BroadcastCampaign campaign = campaignMap.get(recipient.getCampaignId());
            if (campaign == null) {
                log.warn(
                        "Broadcast recipient skipped: campaignId={} recipientId={} not found",
                        recipient.getCampaignId(),
                        recipient.getId()
                );
                broadcastService.markRecipientFailed(recipient, "Кампания не найдена");
                skippedCount++;
                pauseBetweenBroadcastMessages(i, recipients.size());
                continue;
            }

            BroadcastDeliveryResult result = broadcastHandler.copyBroadcastMessage(recipient, campaign);
            if (result.success()) {
                broadcastService.markRecipientSent(recipient, result.sentMessageId());
                sentCount++;
            } else {
                broadcastService.markRecipientFailed(recipient, result.errorMessage());
                failedCount++;
            }

            pauseBetweenBroadcastMessages(i, recipients.size());
        }

        log.info(
                "Broadcast batch finished: recipients={}, sent={}, failed={}, skipped={}",
                recipients.size(),
                sentCount,
                failedCount,
                skippedCount
        );
    }

    private void pauseBetweenBroadcastMessages(int index, int total) {
        if (index >= total - 1 || BROADCAST_SEND_DELAY_MS <= 0) {
            return;
        }

        try {
            Thread.sleep(BROADCAST_SEND_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Broadcast batch interrupted during send delay");
        }
    }
}
