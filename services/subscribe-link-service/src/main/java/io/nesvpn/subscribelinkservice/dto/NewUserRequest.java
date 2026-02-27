package io.nesvpn.subscribelinkservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {

    private String username;

    private Long telegramId;

    private String email;

    private String[] activeInternalSquads;

    private Long trafficLimitBytes;

    private String expireAt;

    private Integer hwidDeviceLimit;


}
