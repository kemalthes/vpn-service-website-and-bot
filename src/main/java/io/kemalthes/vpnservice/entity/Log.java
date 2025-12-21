package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("logs")
public class Log {
    @Id
    private Integer id;
    private String userId;
    private String action;
    private String description;
    private LocalDateTime createdAt;
}