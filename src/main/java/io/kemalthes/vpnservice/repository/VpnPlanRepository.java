package io.kemalthes.vpnservice.repository;

import io.kemalthes.vpnservice.entity.VpnPlan;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface VpnPlanRepository extends CrudRepository<VpnPlan, Integer> {

    List<VpnPlan> findAll();

}
