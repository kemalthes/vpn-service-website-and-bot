package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.notification.DueSubscriptionExpirationNotification;
import io.nesvpn.telegrambot.enums.SubscriptionExpirationNotificationType;
import io.nesvpn.telegrambot.model.SubscriptionExpirationNotification;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.repository.SubscriptionExpirationNotificationRepository;
import io.nesvpn.telegrambot.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionExpirationNotificationService {
    private static final int DUE_LOOKBACK_MINUTES = 60;

    private final SubscriptionExpirationNotificationRepository subscriptionExpirationNotificationRepository;
    private final TokenRepository tokenRepository;

    @Transactional
    public List<DueSubscriptionExpirationNotification> findDueNotifications(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(DUE_LOOKBACK_MINUTES);
        LocalDateTime currentDayStart = getCurrentMoscowDayStart();

        List<DueSubscriptionExpirationNotification> result = new ArrayList<>();
        result.addAll(toDueNotifications(
                tokenRepository.findDueExpiredSubscriptionExpirationTokens(
                        windowStart,
                        now,
                        currentDayStart,
                        alreadySentTypesFor(SubscriptionExpirationNotificationType.EXPIRED),
                        PageRequest.of(0, limit)
                ),
                SubscriptionExpirationNotificationType.EXPIRED
        ));

        int remaining = limit - result.size();
        if (remaining <= 0) {
            return result;
        }

        result.addAll(toDueNotifications(
                tokenRepository.findDueActiveSubscriptionExpirationTokens(
                        windowStart.plusDays(1),
                        now.plusDays(1),
                        currentDayStart,
                        alreadySentTypesFor(SubscriptionExpirationNotificationType.ONE_DAY),
                        PageRequest.of(0, remaining)
                ),
                SubscriptionExpirationNotificationType.ONE_DAY
        ));

        remaining = limit - result.size();
        if (remaining <= 0) {
            return result;
        }

        result.addAll(toDueNotifications(
                tokenRepository.findDueActiveSubscriptionExpirationTokens(
                        windowStart.plusDays(2),
                        now.plusDays(2),
                        currentDayStart,
                        alreadySentTypesFor(SubscriptionExpirationNotificationType.TWO_DAYS),
                        PageRequest.of(0, remaining)
                ),
                SubscriptionExpirationNotificationType.TWO_DAYS
        ));

        return result;
    }

    @Transactional
    public void markNotificationsSent(List<DueSubscriptionExpirationNotification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Map<Long, DueSubscriptionExpirationNotification> notificationsByTokenId = notifications.stream()
                .collect(Collectors.toMap(
                        DueSubscriptionExpirationNotification::tokenId,
                        notification -> notification,
                        (first, second) -> first
                ));

        Map<Long, SubscriptionExpirationNotification> existingNotificationsByTokenId =
                subscriptionExpirationNotificationRepository.findAllByTokenIdIn(notificationsByTokenId.keySet()).stream()
                        .collect(Collectors.toMap(SubscriptionExpirationNotification::getTokenId, notification -> notification));

        List<SubscriptionExpirationNotification> sentNotifications = notificationsByTokenId.values().stream()
                .map(notification -> updateNotification(
                        existingNotificationsByTokenId.get(notification.tokenId()),
                        notification,
                        now
                ))
                .toList();

        subscriptionExpirationNotificationRepository.saveAll(sentNotifications);
    }

    private List<DueSubscriptionExpirationNotification> toDueNotifications(
            List<Token> tokens,
            SubscriptionExpirationNotificationType type
    ) {
        if (tokens.isEmpty()) {
            return List.of();
        }

        return tokens.stream()
                .map(token -> new DueSubscriptionExpirationNotification(
                        token.getId(),
                        token.getUserId(),
                        token.getValidTo(),
                        type
                ))
                .toList();
    }

    private SubscriptionExpirationNotification updateNotification(
            SubscriptionExpirationNotification notification,
            DueSubscriptionExpirationNotification dueNotification,
            LocalDateTime now
    ) {
        if (notification == null) {
            notification = new SubscriptionExpirationNotification();
            notification.setTokenId(dueNotification.tokenId());
            notification.setCreatedAt(now);
        }

        notification.setNotificationType(dueNotification.type());
        notification.setScheduledAt(getScheduledAt(dueNotification.validTo(), dueNotification.type()));
        notification.setTokenValidTo(dueNotification.validTo());
        notification.setSentAt(now);
        return notification;
    }

    private LocalDateTime getScheduledAt(LocalDateTime validTo, SubscriptionExpirationNotificationType type) {
        return switch (type) {
            case TWO_DAYS -> validTo.minusDays(2);
            case ONE_DAY -> validTo.minusDays(1);
            case EXPIRED -> validTo;
        };
    }

    private LocalDateTime getCurrentMoscowDayStart() {
        ZoneId moscow = ZoneId.of("Europe/Moscow");
        return ZonedDateTime.now(moscow)
                .toLocalDate()
                .atStartOfDay(moscow)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private List<SubscriptionExpirationNotificationType> alreadySentTypesFor(SubscriptionExpirationNotificationType type) {
        return switch (type) {
            case TWO_DAYS -> List.of(
                    SubscriptionExpirationNotificationType.TWO_DAYS,
                    SubscriptionExpirationNotificationType.ONE_DAY,
                    SubscriptionExpirationNotificationType.EXPIRED
            );
            case ONE_DAY -> List.of(
                    SubscriptionExpirationNotificationType.ONE_DAY,
                    SubscriptionExpirationNotificationType.EXPIRED
            );
            case EXPIRED -> List.of(SubscriptionExpirationNotificationType.EXPIRED);
        };
    }

}
