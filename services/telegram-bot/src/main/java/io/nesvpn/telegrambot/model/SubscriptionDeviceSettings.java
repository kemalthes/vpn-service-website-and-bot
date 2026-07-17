package io.nesvpn.telegrambot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "subscription_device_settings")
public class SubscriptionDeviceSettings {

    @Id
    @Column(name = "id", nullable = false)
    private Short id;

    @Column(name = "price_per_device_month", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerDeviceMonth;

    @Column(name = "max_devices_limit", nullable = false)
    private Integer maxDevicesLimit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
