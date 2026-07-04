package io.nesvpn.telegrambot.rabbit;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DeviceLimitChangeRequest {

    private UUID userId;

    private Long orderId;

    private Long tokenId;

    private Integer targetMaxDevices;

    private String tgUsername;
}
