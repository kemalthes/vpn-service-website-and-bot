package io.nesvpn.backendsiteservice.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("comments")
@Builder
public class Comment {

    @Id
    private Integer id;

    private String text;

    private Integer score;

    private UUID userId;

    private LocalDateTime createdAt;
}