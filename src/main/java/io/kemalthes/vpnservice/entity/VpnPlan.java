package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Table("vpn_plans")
public class VpnPlan {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    private Integer duration;
    private String country;
}