package io.nesvpn.telegrambot.dto.broadcast;

public record BroadcastDeliveryResult(boolean success, Long sentMessageId, String errorMessage) {
    public static BroadcastDeliveryResult sent(Long sentMessageId) {
        return new BroadcastDeliveryResult(true, sentMessageId, null);
    }

    public static BroadcastDeliveryResult failed(String errorMessage) {
        return new BroadcastDeliveryResult(false, null, errorMessage);
    }
}
