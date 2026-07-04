package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.enums.OrderOperationType;
import io.nesvpn.telegrambot.dto.notification.AutoRenewalResult;
import io.nesvpn.telegrambot.model.SubscriptionAutoRenewalSetting;
import io.nesvpn.telegrambot.model.Token;
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
    private final SubscriptionDevicePricingService pricingService;
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
    public AutoRenewalResult tryRenew(
            User user,
            SubscriptionAutoRenewalSetting setting,
            VpnPlan plan,
            Long tokenId,
            Integer tokenMaxDevices,
            Integer renewalTargetMaxDevices
    ) {
        if (setting == null || !setting.isEnabled()) {
            return new AutoRenewalResult(
                    AutoRenewalResult.Status.DISABLED,
                    plan,
                    user.getBalance(),
                    null,
                    null,
                    tokenMaxDevices,
                    null,
                    null,
                    false);
        }

        VpnPlan renewalPlan = plan != null ? plan : vpnPlanService.findOneMonthPlan().orElse(null);
        if (renewalPlan == null) {
            return new AutoRenewalResult(
                    AutoRenewalResult.Status.PLAN_NOT_FOUND,
                    null,
                    user.getBalance(),
                    null,
                    null,
                    tokenMaxDevices,
                    null,
                    null,
                    false);
        }

        int currentMaxDevices = tokenMaxDevices != null ? tokenMaxDevices : pricingService.defaultDevices(renewalPlan);
        int requestedMaxDevices = renewalTargetMaxDevices != null ? renewalTargetMaxDevices : currentMaxDevices;
        BigDecimal price = pricingService.calculateRenewalPrice(renewalPlan, requestedMaxDevices);
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(price) < 0) {
            return tryFallbackOrInsufficient(user, tokenId, renewalPlan, currentMaxDevices, requestedMaxDevices, price, balance);
        }

        return createAutoRenewalOrder(
                user,
                tokenId,
                renewalPlan,
                currentMaxDevices,
                requestedMaxDevices,
                requestedMaxDevices,
                price,
                balance,
                false,
                true);
    }

    private AutoRenewalResult tryFallbackOrInsufficient(
            User user,
            Long tokenId,
            VpnPlan renewalPlan,
            int currentMaxDevices,
            int requestedMaxDevices,
            BigDecimal requestedPrice,
            BigDecimal balance
    ) {
        int standardDevices = pricingService.defaultDevices(renewalPlan);
        if (requestedMaxDevices > standardDevices) {
            BigDecimal basePrice = pricingService.calculateRenewalPrice(renewalPlan, standardDevices);
            if (balance.compareTo(basePrice) >= 0) {
                return createAutoRenewalOrder(
                        user,
                        tokenId,
                        renewalPlan,
                        currentMaxDevices,
                        requestedMaxDevices,
                        standardDevices,
                        basePrice,
                        balance,
                        true,
                        false);
            }
        }

        return insufficientFunds(
                renewalPlan,
                balance,
                requestedPrice,
                currentMaxDevices,
                requestedMaxDevices,
                requestedMaxDevices);
    }

    private AutoRenewalResult createAutoRenewalOrder(
            User user,
            Long tokenId,
            VpnPlan renewalPlan,
            int currentMaxDevices,
            int requestedMaxDevices,
            int targetMaxDevices,
            BigDecimal price,
            BigDecimal balance,
            boolean fallback,
            boolean retryWithFallback
    ) {
        Token token = new Token();
        token.setId(tokenId);
        try {
            orderService.createImmediatePaidOrder(
                    user,
                    token,
                    renewalPlan,
                    OrderOperationType.AUTO_RENEWAL,
                    targetMaxDevices,
                    price,
                    getBalanceDescription(renewalPlan));
        } catch (NotEnoughBalanceException e) {
            if (retryWithFallback) {
                return tryFallbackOrInsufficient(
                        user,
                        tokenId,
                        renewalPlan,
                        currentMaxDevices,
                        requestedMaxDevices,
                        price,
                        e.getBalance());
            }
            return insufficientFunds(
                    renewalPlan,
                    e.getBalance(),
                    price,
                    currentMaxDevices,
                    targetMaxDevices,
                    requestedMaxDevices);
        }

        return new AutoRenewalResult(
                AutoRenewalResult.Status.SUCCESS,
                renewalPlan,
                balance,
                balance.subtract(price),
                price,
                currentMaxDevices,
                targetMaxDevices,
                requestedMaxDevices,
                fallback);
    }

    private AutoRenewalResult insufficientFunds(
            VpnPlan renewalPlan,
            BigDecimal balance,
            BigDecimal price,
            int currentMaxDevices,
            int targetMaxDevices,
            int requestedMaxDevices
    ) {
        return new AutoRenewalResult(
                AutoRenewalResult.Status.INSUFFICIENT_FUNDS,
                renewalPlan,
                balance,
                null,
                price,
                currentMaxDevices,
                targetMaxDevices,
                requestedMaxDevices,
                false);
    }

    public String getBalanceDescription(VpnPlan plan) {
        return "Автопродление подписки на " + plan.getName();
    }
}
