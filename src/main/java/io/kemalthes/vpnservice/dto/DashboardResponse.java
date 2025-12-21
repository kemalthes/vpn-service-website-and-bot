package io.kemalthes.vpnservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Main page info")
public class DashboardResponse {

    private long totalOrders;

    private long activeSubscriptions;

    private long totalUsers;

    private double averageScore;

    private List<CommentResponse> featuredComments;
}