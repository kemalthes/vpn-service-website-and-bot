package io.nesvpn.telegrambot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
public class CryptoPayment {
    private String transactionId;
    private String tonLink;
    private String qrCodeBase64;
    private double amountRub;
    private double amountUsdt;
    private String walletAddress;
    private long expiresAt;
}