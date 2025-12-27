package io.kemalthes.vpnservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Vpn plan features")
public class VpnPlanFeatureResponse {

    String featureKey;

    String value;
}
