package io.nesvpn.subscribelinkservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HwidDeleteRequest {

    private String userUuid;

    private String hwid;
}

