package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.repository.VpnPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class VpnPlanService {
    private static final Long ONE_MONTH_PLAN_ID = 1L;
    private static final String BONUS_COUNTRY = "Bonus";

    private final VpnPlanRepository vpnPlanRepository;

    public List<VpnPlan> getAllPlans() {
        return getPurchasableSubscriptionPlans();
    }

    public List<VpnPlan> getPurchasableSubscriptionPlans() {
        return vpnPlanRepository.findByPriceGreaterThanAndCountryNotOrderByDurationAsc(0, BONUS_COUNTRY);
    }

    public List<VpnPlan> getPlansByCountry(String country) {
        return vpnPlanRepository.findByCountry(country);
    }

    public Optional<VpnPlan> findById(Long id) {
        return vpnPlanRepository.findById(id);
    }

    public Optional<VpnPlan> findOneMonthPlan() {
        return vpnPlanRepository.findById(ONE_MONTH_PLAN_ID);
    }

    public Optional<VpnPlan> findLucky777Plan(Integer duration) {
        return vpnPlanRepository.findFirstByCountryAndDurationOrderByIdAsc(BONUS_COUNTRY, duration);
    }
}
