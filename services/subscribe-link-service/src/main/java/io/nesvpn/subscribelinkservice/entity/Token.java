package io.nesvpn.subscribelinkservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    private String token;

    private Date createdAt;

    private Date validTo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private UUID vpnPanelUserUuid;
}
