package io.nesvpn.telegrambot.dto.notification;

import io.nesvpn.telegrambot.enums.SubscriptionExpirationNotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DueSubscriptionExpirationNotification(
        Long tokenId,
        UUID userId,
        LocalDateTime validTo,
        SubscriptionExpirationNotificationType type
) {
}
