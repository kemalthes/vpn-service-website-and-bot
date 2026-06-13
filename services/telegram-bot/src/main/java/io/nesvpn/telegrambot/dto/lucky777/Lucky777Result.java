package io.nesvpn.telegrambot.dto.lucky777;

import java.time.Duration;

public record Lucky777Result(
        Integer diceValue,
        Integer rewardDays,
        String result,
        boolean processed,
        boolean noToken,
        Duration remaining
) {
    public static Lucky777Result processed(Integer diceValue, Integer rewardDays, String result) {
        return new Lucky777Result(diceValue, rewardDays, result, true, false, Duration.ZERO);
    }

    public static Lucky777Result cooldown(Integer diceValue, Duration remaining) {
        return new Lucky777Result(diceValue, 0, "COOLDOWN", false, false, remaining);
    }

    public static Lucky777Result noToken(Integer diceValue) {
        return new Lucky777Result(diceValue, 0, "NO_TOKEN", false, true, Duration.ZERO);
    }
}
