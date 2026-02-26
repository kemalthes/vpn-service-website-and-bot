package io.nesvpn.subscribelinkservice.repository;

import io.nesvpn.subscribelinkservice.entity.VpnPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VpnPlanRepository extends JpaRepository<VpnPlan, Long> {
}
