package io.nesvpn.subscribelinkservice.rabbit;

import lombok.Data;

import java.util.UUID;

@Data
public class DeviceLimitChangeRequest {

    private UUID userId;

    private Long orderId;

    private Long tokenId;

    private Integer targetMaxDevices;

    private String tgUsername;
}
