package io.kemalthes.vpnservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("comments")
public class Comment {
    @Id
    private String id;
    private String text;
    private Integer score;
    private String userId;
}