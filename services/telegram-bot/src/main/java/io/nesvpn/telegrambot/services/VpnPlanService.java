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

    private final VpnPlanRepository vpnPlanRepository;

    public List<VpnPlan> getAllPlans() {
        return vpnPlanRepository.findAllByOrderByDurationAsc();
    }

    public List<VpnPlan> getPlansByCountry(String country) {
        return vpnPlanRepository.findByCountry(country);
    }

    public Optional<VpnPlan> findById(Long id) {
        return vpnPlanRepository.findById(id);
    }
}
