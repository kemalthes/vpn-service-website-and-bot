package io.nesvpn.telegrambot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "vpn_plan_id")
    private Long vpnPlanId;

    @Column(name = "token_id")
    private Long tokenId;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "operation_type", length = 32)
    private String operationType;

    @Column(name = "target_max_devices")
    private Integer targetMaxDevices;

    @Column(name = "total_price", precision = 12, scale = 0)
    private BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
