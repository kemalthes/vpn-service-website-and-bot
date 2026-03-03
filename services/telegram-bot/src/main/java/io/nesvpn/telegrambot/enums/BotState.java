package io.nesvpn.telegrambot.enums;

public enum BotState {
    START,
    PROFILE,
    REFERRALS,
    SUBSCRIPTIONS,
    SUBSCRIPTIONS_EXTEND,
    SUBSCRIPTIONS_CONFIRM,
    INFO,
    BALANCE,
    BALANCE_HISTORY,
    BALANCE_TOP_UP,
    BALANCE_AWAITING_AMOUNT,
    BALANCE_AWAITING_AMOUNT_CRYPTO,
    PAYMENT_AWAITING_CONFIRMATION,
    PAYMENT_AWAITING_CONFIRMATION_CRYPTO,
    INSTRUCTIONS,
    INSTRUCTIONS_WINDOWS,
    INSTRUCTIONS_MACOS,
    INSTRUCTIONS_ANDROID,
    INSTRUCTIONS_IOS;


    @Override
    public String toString() {
        return this.name();
    }

    public static BotState fromString(String state) {
        try {
            return BotState.valueOf(state);
        } catch (IllegalArgumentException e) {
            return START;
        }
    }
}
