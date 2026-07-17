package io.nesvpn.telegrambot.dto.notification;

import io.nesvpn.telegrambot.model.VpnPlan;

import java.math.BigDecimal;

public record AutoRenewalResult(
        Status status,
        VpnPlan plan,
        BigDecimal balance,
        BigDecimal balanceAfter,
        BigDecimal chargedAmount,
        Integer oldMaxDevices,
        Integer targetMaxDevices,
        Integer requestedMaxDevices,
        boolean deviceLimitFallback
) {

    public enum Status {
        DISABLED,
        SUCCESS,
        INSUFFICIENT_FUNDS,
        PLAN_NOT_FOUND
    }
}
