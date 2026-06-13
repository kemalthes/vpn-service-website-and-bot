package io.nesvpn.telegrambot.enums;

public enum TransactionType {
    TOP_UP("Пополнение"),
    REFERRAL_BONUS("Награда за друга"),
    PROMO_CODE("Промокод"),
    SUBSCRIPTION_PURCHASE("Покупка подписки"),
    LUCKY_777_WIN("777 бонус"),
    REFUND("Возврат");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
