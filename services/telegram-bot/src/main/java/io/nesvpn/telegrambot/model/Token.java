package io.nesvpn.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)  // Изменено
    private User user;

    public boolean isActive() {
        return "active".equals(status.toLowerCase()) && validTo != null && validTo.isAfter(LocalDateTime.now());
    }
}