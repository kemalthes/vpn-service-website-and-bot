package io.nesvpn.telegrambot.model;

public enum BotState {
    START,
    PROFILE,
    REFERRALS,
    SUBSCRIPTIONS,
    SUBSCRIPTIONS_EXTEND,
    SUBSCRIPTIONS_CONFIRM,
    BALANCE,
    BALANCE_HISTORY,
    BALANCE_TOP_UP,
    BALANCE_AWAITING_AMOUNT,
    PAYMENT_AWAITING_CONFIRMATION,
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
