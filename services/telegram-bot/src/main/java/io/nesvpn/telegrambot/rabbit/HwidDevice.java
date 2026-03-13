package io.nesvpn.telegrambot.rabbit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HwidDevice {

    private String hwid;

    private UUID userUuid;

    private String platform;

    private String osVersion;

    private String deviceModel;

    private String userAgent;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
