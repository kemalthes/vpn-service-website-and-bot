package io.nesvpn.telegrambot.enums;

public enum OrderStatus {
    DRAFT("DRAFT"),
    PENDING("PENDING"),
    PAID("PAID"),
    CANCELLED("CANCELLED"),
    PROVIDED("PROVIDED");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OrderStatus fromString(String text) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.value.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + text);
    }
}
