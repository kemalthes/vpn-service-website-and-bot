package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.VpnPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VpnPlanRepository extends JpaRepository<VpnPlan, Long> {

    List<VpnPlan> findAllByOrderByDurationAsc();

    List<VpnPlan> findByPriceGreaterThanAndCountryNotOrderByDurationAsc(Integer price, String country);

    List<VpnPlan> findByCountry(String country);

    Optional<VpnPlan> findFirstByCountryAndDurationOrderByIdAsc(String country, Integer duration);
}
