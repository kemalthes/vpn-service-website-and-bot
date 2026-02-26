package io.nesvpn.subscribelinkservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
