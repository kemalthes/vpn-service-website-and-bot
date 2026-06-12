package io.nesvpn.telegrambot.dto.lucky777;

import java.time.LocalDateTime;
import java.util.UUID;

public record Lucky777AvailableNotification(UUID userId, LocalDateTime lastSpinAt) {
}
