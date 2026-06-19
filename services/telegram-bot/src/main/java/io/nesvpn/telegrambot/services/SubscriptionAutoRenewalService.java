package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.notification.AutoRenewalResult;
import io.nesvpn.telegrambot.model.SubscriptionAutoRenewalSetting;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.repository.SubscriptionAutoRenewalSettingRepository;
import io.nesvpn.telegrambot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionAutoRenewalService {
    private final SubscriptionAutoRenewalSettingRepository subscriptionAutoRenewalSettingRepository;
    private final VpnPlanService vpnPlanService;
    private final OrderService orderService;
    private final UserRepository userRepository;

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

    @Transactional
    public void toggle(UUID userId) {
        SubscriptionAutoRenewalSetting setting = subscriptionAutoRenewalSettingRepository.findById(userId)
                .orElseGet(() -> {
                    SubscriptionAutoRenewalSetting newSetting = new SubscriptionAutoRenewalSetting();
                    User user = userRepository.getReferenceById(userId);
                    newSetting.setUser(user);
                    return newSetting;
                });
        setting.setEnabled(!setting.isEnabled());
        subscriptionAutoRenewalSettingRepository.save(setting);
    }

    @Transactional
    public AutoRenewalResult tryRenew(User user, SubscriptionAutoRenewalSetting setting, VpnPlan plan) {
        if (setting == null || !setting.isEnabled()) {
            return new AutoRenewalResult(AutoRenewalResult.Status.DISABLED, plan, user.getBalance(), null);
        }

        VpnPlan renewalPlan = plan != null ? plan : vpnPlanService.findOneMonthPlan().orElse(null);
        if (renewalPlan == null) {
            return new AutoRenewalResult(AutoRenewalResult.Status.PLAN_NOT_FOUND, null, user.getBalance(), null);
        }

        BigDecimal price = BigDecimal.valueOf(renewalPlan.getPrice());
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(price) < 0) {
            return new AutoRenewalResult(AutoRenewalResult.Status.INSUFFICIENT_FUNDS, renewalPlan, balance, null);
        }

        orderService.createOrder(user, renewalPlan, getBalanceDescription(renewalPlan));
        return new AutoRenewalResult(AutoRenewalResult.Status.SUCCESS, renewalPlan, balance, balance.subtract(price));
    }

    public String getBalanceDescription(VpnPlan plan) {
        return "Автопродление подписки на " + plan.getName();
    }
}
