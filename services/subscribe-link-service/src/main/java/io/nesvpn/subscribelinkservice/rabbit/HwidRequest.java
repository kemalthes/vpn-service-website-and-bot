package io.nesvpn.subscribelinkservice.rabbit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HwidRequest {

    private UUID userId;

    private HwidMethod method;

    private String hwid;
}
