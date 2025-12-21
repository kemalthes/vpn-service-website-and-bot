package io.kemalthes.vpnservice.mapper;

import io.kemalthes.vpnservice.dto.VpnPlanResponse;
import io.kemalthes.vpnservice.entity.VpnPlan;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VpnPlanMapper {

    List<VpnPlanResponse> toDtoList(List<VpnPlan> vpnPlan);

}
