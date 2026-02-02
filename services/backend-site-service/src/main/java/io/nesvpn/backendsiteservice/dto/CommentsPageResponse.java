package io.nesvpn.backendsiteservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Comments data")
public class CommentsPageResponse {

    private Long count;

    private Double averageScore;

    private int size;

    private int page;

    private int totalPages;

    private List<CommentResponse> comments;
}
