package io.nesvpn.backendsiteservice.mapper;

import io.nesvpn.backendsiteservice.dto.VpnPlanResponse;
import io.nesvpn.backendsiteservice.entity.VpnPlan;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VpnPlanMapper {

    List<VpnPlanResponse> toDtoList(List<VpnPlan> vpnPlan);

}
