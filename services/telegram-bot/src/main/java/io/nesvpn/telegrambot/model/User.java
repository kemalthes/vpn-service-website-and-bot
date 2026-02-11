package io.nesvpn.telegrambot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tg_id")
    private Long tgId;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "role", length = 16, nullable = false)
    private String role;

    @Column(name = "referral_code", length = 64)
    private String referralCode;

    @Column(name = "referred_by")
    private Long referredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by", referencedColumnName = "tg_id",
            insertable = false, updatable = false)
    private User referrer;

    @OneToMany(mappedBy = "referrer", fetch = FetchType.LAZY)
    private List<User> referrals;

    @Column(name = "balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
