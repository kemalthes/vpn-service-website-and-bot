package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.notification.AutoRenewalResult;
import io.nesvpn.telegrambot.model.SubscriptionAutoRenewalSetting;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.repository.SubscriptionAutoRenewalSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionAutoRenewalService {
    private final SubscriptionAutoRenewalSettingRepository subscriptionAutoRenewalSettingRepository;
    private final VpnPlanService vpnPlanService;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId) {
        return subscriptionAutoRenewalSettingRepository.findById(userId)
                .map(SubscriptionAutoRenewalSetting::isEnabled)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<UUID, SubscriptionAutoRenewalSetting> getSettingsByUserIds(Collection<UUID> userIds) {
        return subscriptionAutoRenewalSettingRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(SubscriptionAutoRenewalSetting::getUserId, setting -> setting));
    }

    @Transactional(readOnly = true)
    public Optional<VpnPlan> findOneMonthPlan() {
        return vpnPlanService.findOneMonthPlan();
    }

    @Transactional
    public boolean toggle(User user) {
        return Boolean.TRUE.equals(subscriptionAutoRenewalSettingRepository.toggleEnabled(user.getId()));
    }

    @Transactional
    public AutoRenewalResult tryRenew(User user) {
        SubscriptionAutoRenewalSetting setting = subscriptionAutoRenewalSettingRepository.findById(user.getId())
                .orElse(null);
        if (setting == null || !setting.isEnabled()) {
            return new AutoRenewalResult(AutoRenewalResult.Status.DISABLED, null, user.getBalance(), null);
        }

        VpnPlan plan = vpnPlanService.findOneMonthPlan().orElse(null);
        return tryRenew(user, setting, plan);
    }

    @Transactional
    public AutoRenewalResult tryRenew(User user, SubscriptionAutoRenewalSetting setting, VpnPlan plan) {
        if (setting == null || !setting.isEnabled()) {
            return new AutoRenewalResult(AutoRenewalResult.Status.DISABLED, plan, user.getBalance(), null);
        }
        if (plan == null) {
            return new AutoRenewalResult(AutoRenewalResult.Status.PLAN_NOT_FOUND, null, user.getBalance(), null);
        }

        BigDecimal price = BigDecimal.valueOf(plan.getPrice());
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(price) < 0) {
            return new AutoRenewalResult(AutoRenewalResult.Status.INSUFFICIENT_FUNDS, plan, balance, null);
        }

        orderService.createOrder(user, plan, getBalanceDescription(plan));
        return new AutoRenewalResult(AutoRenewalResult.Status.SUCCESS, plan, balance, balance.subtract(price));
    }

    public String getBalanceDescription(VpnPlan plan) {
        return "Автопродление подписки на " + plan.getName();
    }
}
