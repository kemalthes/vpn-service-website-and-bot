package io.nesvpn.backendsiteservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("vpn_plan_features")
public class VpnPlanFeature {

    @Id
    private String featureKey;

    private String value;
}