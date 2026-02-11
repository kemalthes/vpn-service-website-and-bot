package io.nesvpn.subscribelinkservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    private UUID id;

    private Long tgId;

    private String name;

    private String email;

    private String password;

    private String role;

    private String referralCode;

    private Long referredBy;

    private BigDecimal balance = BigDecimal.ZERO;

    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "user", orphanRemoval = true)
    private Token token;
}
