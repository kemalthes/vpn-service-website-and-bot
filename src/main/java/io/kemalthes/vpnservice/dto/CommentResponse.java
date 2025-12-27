package io.kemalthes.vpnservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Comment")
public class CommentResponse {

    private String text;

    private Integer score;

    private UUID userId;

    private String userName;
}
