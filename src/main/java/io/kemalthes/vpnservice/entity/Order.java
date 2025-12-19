package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("orders")
public class Order {
    @Id
    private String id;
    private String userId;
    private String vpnPlanId;
    private String status;
    private LocalDateTime createdAt;
}
