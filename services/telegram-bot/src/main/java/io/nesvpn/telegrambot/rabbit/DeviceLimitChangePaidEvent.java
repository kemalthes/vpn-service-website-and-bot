package io.nesvpn.telegrambot.rabbit;

import java.util.UUID;

public record DeviceLimitChangePaidEvent(UUID userId, Long orderId, Long tokenId, Integer targetMaxDevices, Long tgId) {
}
