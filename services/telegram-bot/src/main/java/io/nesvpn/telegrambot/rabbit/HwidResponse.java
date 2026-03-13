package io.nesvpn.telegrambot.rabbit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HwidResponse {

    private UUID userId;

    private HwidMethod method;

    private boolean success;

    private String message;

    private String hwid;

    private Integer total;

    private List<HwidDevice> devices;
}
