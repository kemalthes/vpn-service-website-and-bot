package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("payments")
public class Payment {
    @Id
    private String id;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paymentDate;
    private String method;
    private String transactionToken;
    private String currency;
}
