package io.nesvpn.telegrambot.dto;

import lombok.Data;

@Data
public class PlategaTransactionResponse {

    private String id;
    private String status;
    private PaymentDetails paymentDetails;
    private String paymentMethod;
    private String expiresIn;
    private String qr;
    private String payload;
    private String description;

    @Data
    public static class PaymentDetails {
        private Double amount;
        private String currency;
    }
}