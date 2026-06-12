package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.lucky777.Lucky777AvailableNotification;
import io.nesvpn.telegrambot.dto.lucky777.Lucky777Result;
import io.nesvpn.telegrambot.dto.lucky777.Lucky777Status;
import io.nesvpn.telegrambot.model.Lucky777Spin;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.repository.Lucky777SpinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Lucky777Service {

    public static final int JACKPOT_DICE_VALUE = 64;
    public static final int JACKPOT_REWARD_DAYS = 3;
    public static final int TRIPLE_REWARD_DAYS = 1;
    private static final Duration SPIN_COOLDOWN = Duration.ofHours(12);

    private final Lucky777SpinRepository lucky777SpinRepository;
    private final TokenService tokenService;
    private final VpnPlanService vpnPlanService;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public Lucky777Status getStatus(User user) {
        Lucky777Spin spin = lucky777SpinRepository.findByUserId(user.getId()).orElse(null);
        if (spin == null || spin.getLastSpinAt() == null) {
            return new Lucky777Status(true, Duration.ZERO);
        }

        Duration remaining = getRemaining(spin.getLastSpinAt(), LocalDateTime.now());
        return new Lucky777Status(remaining.isZero(), remaining);
    }

    @Transactional
    public Lucky777Result processDice(User user, Integer diceValue) {
        Token token = tokenService.getUserToken(user.getId());
        if (token == null) {
            return Lucky777Result.noToken(diceValue);
        }

        lucky777SpinRepository.insertIfMissing(user.getId());
        Lucky777Spin spin = lucky777SpinRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalStateException("Lucky 777 spin row not found"));

        LocalDateTime now = LocalDateTime.now();
        Duration remaining = getRemaining(spin.getLastSpinAt(), now);
        if (!remaining.isZero()) {
            return Lucky777Result.cooldown(diceValue, remaining);
        }

        int rewardDays = getRewardDays(diceValue);
        String result = getResult(rewardDays);

        spin.setLastSpinAt(now);
        spin.setLastDiceValue(diceValue);
        spin.setLastRewardDays(rewardDays);
        spin.setLastResult(result);
        spin.setTotalSpins(getSafeCount(spin.getTotalSpins()) + 1);

        if (rewardDays > 0) {
            spin.setTotalWins(getSafeCount(spin.getTotalWins()) + 1);
            VpnPlan plan = vpnPlanService.findLucky777Plan(rewardDays)
                    .orElseThrow(() -> new IllegalStateException("Lucky 777 plan not found for duration " + rewardDays));
            orderService.createLucky777BonusOrder(user, plan, diceValue);
        }

        lucky777SpinRepository.save(spin);
        return Lucky777Result.processed(diceValue, rewardDays, result);
    }

    @Transactional
    public List<Lucky777AvailableNotification> findAvailableNotifications(int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime availableBefore = now.minus(SPIN_COOLDOWN);

        List<Lucky777Spin> spins = lucky777SpinRepository.findAvailableForNotificationForUpdate(
                availableBefore,
                PageRequest.of(0, limit)
        );

        if (spins.isEmpty()) {
            return List.of();
        }

        return spins.stream()
                .map(spin -> new Lucky777AvailableNotification(spin.getUserId(), spin.getLastSpinAt()))
                .toList();
    }

    @Transactional
    public void markAvailableNotificationsSent(List<Lucky777AvailableNotification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        Map<UUID, Lucky777AvailableNotification> notificationsByUserId = notifications.stream()
                .collect(Collectors.toMap(
                        Lucky777AvailableNotification::userId,
                        notification -> notification,
                        (first, second) -> first
                ));

        List<Lucky777Spin> spins = lucky777SpinRepository.findAllByUserIdInForUpdate(notificationsByUserId.keySet());
        LocalDateTime now = LocalDateTime.now();

        List<Lucky777Spin> unchangedSpins = spins.stream()
                .filter(spin -> {
                    Lucky777AvailableNotification notification = notificationsByUserId.get(spin.getUserId());
                    return notification != null && notification.lastSpinAt().equals(spin.getLastSpinAt());
                })
                .peek(spin -> spin.setLastAvailableNotificationAt(now))
                .toList();

        lucky777SpinRepository.saveAll(unchangedSpins);
    }

    public boolean isSlotDice(String emoji) {
        return "🎰".equals(emoji);
    }

    private Duration getRemaining(LocalDateTime lastSpinAt, LocalDateTime now) {
        if (lastSpinAt == null) {
            return Duration.ZERO;
        }

        LocalDateTime nextSpinAt = lastSpinAt.plus(SPIN_COOLDOWN);
        if (!nextSpinAt.isAfter(now)) {
            return Duration.ZERO;
        }

        return Duration.between(now, nextSpinAt);
    }

    private int getRewardDays(Integer diceValue) {
        if (diceValue == null) {
            return 0;
        }
        if (diceValue == JACKPOT_DICE_VALUE) {
            return JACKPOT_REWARD_DAYS;
        }
        if (diceValue == 1 || diceValue == 22 || diceValue == 43) {
            return TRIPLE_REWARD_DAYS;
        }
        return 0;
    }

    private int getSafeCount(Integer value) {
        return value != null ? value : 0;
    }

    private String getResult(int rewardDays) {
        if (rewardDays == JACKPOT_REWARD_DAYS) {
            return "JACKPOT_777";
        }
        if (rewardDays == TRIPLE_REWARD_DAYS) {
            return "TRIPLE";
        }
        return "LOSE";
    }

}
