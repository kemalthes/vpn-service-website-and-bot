package io.nesvpn.telegrambot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlategaCreateRequest {

    private Integer paymentMethod;
    private PaymentDetails paymentDetails;
    private String description;
    private String payload;

    @JsonProperty("return")
    private String returnUrl;

    private String failedUrl;

    @Data
    public static class PaymentDetails {
        private Double amount;
        private String currency;
    }
}