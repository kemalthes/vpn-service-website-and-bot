package io.nesvpn.backendsiteservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.List;

@Data
@Table("vpn_plans")
public class VpnPlan {

    @Id
    private Integer id;

    private String name;

    private BigDecimal price;

    private Integer duration;

    private String country;

    @MappedCollection(idColumn = "vpn_plan_id", keyColumn = "feature_key")
    private List<VpnPlanFeature> features;
}