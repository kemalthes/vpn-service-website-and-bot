package io.kemalthes.vpnservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Vpn plan")
public class VpnPlanResponse {

    private String name;

    private BigDecimal price;

    private Integer duration;

    private String country;
}
