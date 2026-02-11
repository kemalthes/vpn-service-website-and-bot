package io.nesvpn.telegrambot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "telegram_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUser {

    @Id
    @Column(name = "tg_id", nullable = false)
    private Long tgId;

    @Column(name = "state", length = 64, nullable = false)
    private String state;

    @Column(name = "previous_state", length = 64)
    private String previousState;
}
