package io.nesvpn.subscribelinkservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateUserRequest {

    private String uuid;

    private Long trafficLimitBytes;

    private String expireAt;

    private Integer hwidDeviceLimit;
}