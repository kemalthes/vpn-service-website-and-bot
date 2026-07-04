package io.nesvpn.telegrambot.services;

import java.math.BigDecimal;

public class NotEnoughBalanceException extends RuntimeException {
    private final BigDecimal requiredAmount;
    private final BigDecimal balance;

    public NotEnoughBalanceException(BigDecimal requiredAmount, BigDecimal balance) {
        super("Not enough balance");
        this.requiredAmount = requiredAmount;
        this.balance = balance;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
