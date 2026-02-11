package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.VpnPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VpnPlanRepository extends JpaRepository<VpnPlan, Long> {

    List<VpnPlan> findAllByOrderByDurationAsc();

    List<VpnPlan> findByCountry(String country);
}