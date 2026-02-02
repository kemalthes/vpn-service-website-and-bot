package io.nesvpn.backendsiteservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Vpn plan")
public class VpnPlanResponse {

    private Integer id;

    private String name;

    private BigDecimal price;

    private Integer duration;

    private String country;

    List<VpnPlanFeatureResponse> features;
}
