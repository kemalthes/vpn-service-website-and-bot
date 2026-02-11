package io.nesvpn.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vpn_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VpnPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "duration", nullable = false)
    private Integer duration;  // В днях (180 дней = 6 месяцев)

    @Column(name = "country", nullable = false)
    private String country;

    public String getDisplayName() {
        int months = duration / 30;
        return String.format("%s (%d мес.)", name, months);
    }

    public int getMonths() {
        return duration / 30;
    }
}
