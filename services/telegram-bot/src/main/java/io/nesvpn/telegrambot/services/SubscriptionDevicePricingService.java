package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.model.SubscriptionDeviceSettings;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.repository.SubscriptionDeviceSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SubscriptionDevicePricingService {
    private static final short SETTINGS_ID = 1;
    private static final int FALLBACK_DEFAULT_DEVICES = 3;
    private static final BigDecimal DAYS_IN_MONTH = BigDecimal.valueOf(30);

    private final SubscriptionDeviceSettingsRepository settingsRepository;

    @Transactional(readOnly = true)
    public SubscriptionDeviceSettings getSettings() {
        return settingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> new EntityNotFoundException("Subscription device settings not found"));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateRenewalPrice(VpnPlan plan, Integer targetMaxDevices) {
        SubscriptionDeviceSettings settings = getSettings();
        int defaultDevices = defaultDevices(plan);
        int target = targetMaxDevices != null ? targetMaxDevices : defaultDevices;
        int extraDevices = Math.max(0, target - defaultDevices);

        BigDecimal extraPrice = settings.getPricePerDeviceMonth()
                .multiply(BigDecimal.valueOf(extraDevices))
                .multiply(BigDecimal.valueOf(plan.getDuration()))
                .divide(DAYS_IN_MONTH, 8, RoundingMode.HALF_UP);

        return BigDecimal.valueOf(plan.getPrice())
                .add(extraPrice)
                .setScale(0, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDeviceLimitChangePrice(Token token, Integer targetMaxDevices) {
        SubscriptionDeviceSettings settings = getSettings();
        int currentMaxDevices = maxDevices(token);
        int addedDevices = Math.max(0, targetMaxDevices - currentMaxDevices);
        BigDecimal daysLeft = daysLeft(token);

        return settings.getPricePerDeviceMonth()
                .multiply(BigDecimal.valueOf(addedDevices))
                .multiply(daysLeft)
                .divide(DAYS_IN_MONTH, 8, RoundingMode.HALF_UP)
                .setScale(0, RoundingMode.HALF_UP);
    }

    public int resolveRenewalTargetMaxDevices(Token token) {
        if (token.getRenewalTargetMaxDevices() != null) {
            return token.getRenewalTargetMaxDevices();
        }
        return maxDevices(token);
    }

    public int maxDevices(Token token) {
        return token.getMaxDevices() != null ? token.getMaxDevices() : FALLBACK_DEFAULT_DEVICES;
    }

    public int defaultDevices(VpnPlan plan) {
        return plan.getDefaultDevices() != null ? plan.getDefaultDevices() : FALLBACK_DEFAULT_DEVICES;
    }

    public int singleDefaultDevices(Collection<VpnPlan> plans) {
        return plans.stream()
                .map(this::defaultDevices)
                .filter(Objects::nonNull)
                .distinct()
                .reduce((first, second) -> {
                    throw new IllegalStateException("Paid plans have different default_devices values");
                })
                .orElse(FALLBACK_DEFAULT_DEVICES);
    }

    private BigDecimal daysLeft(Token token) {
        if (token.getValidTo() == null || !token.getValidTo().isAfter(LocalDateTime.now())) {
            return BigDecimal.ZERO;
        }

        long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), token.getValidTo());
        return BigDecimal.valueOf(Math.max(0, minutes))
                .divide(BigDecimal.valueOf(60 * 24), 8, RoundingMode.HALF_UP);
    }
}
