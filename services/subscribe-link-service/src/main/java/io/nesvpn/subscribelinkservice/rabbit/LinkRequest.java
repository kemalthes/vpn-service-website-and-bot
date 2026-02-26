package io.nesvpn.subscribelinkservice.rabbit;

import lombok.Data;

import java.util.UUID;

@Data
public class LinkRequest {

    private UUID userId;

    private Long orderId;

    private Long planId;

}
