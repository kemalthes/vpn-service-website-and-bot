package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("users")
public class User {

    @Id
    private UUID id;

    private String name;

    private String email;

    private String password;

    private String role;

    private LocalDateTime createdAt;
}
