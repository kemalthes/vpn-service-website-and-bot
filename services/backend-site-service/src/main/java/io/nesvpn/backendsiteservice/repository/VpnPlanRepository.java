package io.nesvpn.backendsiteservice.repository;

import io.nesvpn.backendsiteservice.entity.VpnPlan;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface VpnPlanRepository extends CrudRepository<VpnPlan, Integer> {

    List<VpnPlan> findAll();

}
