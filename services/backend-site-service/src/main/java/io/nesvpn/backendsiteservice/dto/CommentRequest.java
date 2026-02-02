package io.nesvpn.backendsiteservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "New comment request")
public class CommentRequest {

    @NotBlank(message = "Текст обязателен")
    @Size(min = 4, max = 1000, message = "Текст 4-1000 символов")
    private String text;

    @NotNull
    @Min(1) @Max(5)
    private Integer score;

    @NotNull
    private UUID userId;

}
