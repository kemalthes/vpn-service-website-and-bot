package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("tokens")
public class Token {
    @Id
    private String id;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime validTo;
    private String status;
    private String userId;
}