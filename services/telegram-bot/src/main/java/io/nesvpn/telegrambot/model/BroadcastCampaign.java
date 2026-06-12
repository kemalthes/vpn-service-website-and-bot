package io.nesvpn.telegrambot.model;

import io.nesvpn.telegrambot.enums.BroadcastCampaignSource;
import io.nesvpn.telegrambot.enums.BroadcastCampaignStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "broadcast_campaigns")
@Getter
@Setter
@NoArgsConstructor
public class BroadcastCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private BroadcastCampaignSource source;

    @Column(name = "source_chat_id", nullable = false)
    private Long sourceChatId;

    @Column(name = "source_message_id", nullable = false)
    private Integer sourceMessageId;

    @Column(name = "admin_tg_id")
    private Long adminTgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BroadcastCampaignStatus status;

    @Column(name = "total_recipients", nullable = false)
    private Integer totalRecipients;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = BroadcastCampaignStatus.PROCESSING;
        }
        if (totalRecipients == null) {
            totalRecipients = 0;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
