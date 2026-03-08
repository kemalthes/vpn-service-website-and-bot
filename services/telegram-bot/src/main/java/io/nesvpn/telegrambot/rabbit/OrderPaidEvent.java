package io.nesvpn.telegrambot.rabbit;

import java.util.UUID;

public record OrderPaidEvent(UUID userId, Long orderId, Long planId, Long tgId) {}
