package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("logs")
public class Log {
    @Id
    private String id;
    private String userId;
    private String action;
    private String description;
}