package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("orders")
public class Order {

    @Id
    private Integer id;

    private UUID userId;

    private String vpnPlanId;

    private String status;

    private LocalDateTime createdAt;
}
