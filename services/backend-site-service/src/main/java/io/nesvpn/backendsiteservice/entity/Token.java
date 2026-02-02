package io.nesvpn.backendsiteservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("tokens")
public class Token {

    @Id
    private Integer id;

    private String token;

    private LocalDateTime createdAt;

    private LocalDateTime validTo;

    private String status;

    private UUID userId;
}