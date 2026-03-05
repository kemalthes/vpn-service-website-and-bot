package io.nesvpn.telegrambot.dto;

import lombok.Data;

@Data
public class PlategaCreateResponse {
    private String transactionId;
    private String status;
    private String expiresIn;
    private String redirect;
}