package io.nesvpn.telegrambot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lucky_777_spins")
@Getter
@Setter
@NoArgsConstructor
public class Lucky777Spin {

    @Id
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "last_spin_at")
    private LocalDateTime lastSpinAt;

    @Column(name = "last_dice_value")
    private Integer lastDiceValue;

    @Column(name = "last_reward_days", nullable = false)
    private Integer lastRewardDays = 0;

    @Column(name = "last_result", length = 32)
    private String lastResult;

    @Column(name = "last_available_notification_at")
    private LocalDateTime lastAvailableNotificationAt;

    @Column(name = "total_spins", nullable = false)
    private Integer totalSpins = 0;

    @Column(name = "total_wins", nullable = false)
    private Integer totalWins = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
