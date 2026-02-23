package io.nesvpn.telegrambot.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class TonPaymentService {
    @Value("${ton.usdt-master-address}")
    private String usdtMasterAddress;

    @Value("${ton.wallet-address}")
    private String walletAddress;

    @Value("${ton.api-key}")
    private String apiKey;

    private final FloatRatesService floatRatesService;

    public CryptoPayment createUsdtPayment(Payment payment) {
        try {
            double usdRate = floatRatesService.getUsdToRubRate();

            double amountUsdt = payment.getAmount().doubleValue();
            double amountRub = Math.round((usdRate * amountUsdt) * 100.0) / 100.0;

            String transactionId = payment.getTransactionToken();

            String tonLink = generateTonTransferLink(amountUsdt, transactionId);
            String qrBase64 = generateQRCode(tonLink);

            long expiresAt = payment.getExpiresAt().atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            return CryptoPayment.builder()
                    .transactionId(transactionId)
                    .tonLink(tonLink)
                    .qrCodeBase64(qrBase64)
                    .amountRub(amountRub)
                    .amountUsdt(amountUsdt)
                    .walletAddress(walletAddress)
                    .expiresAt(expiresAt)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment for payment id: " + payment.getId(), e);
        }
    }

    private String generateTonTransferLink(double amountUsdt, String memo) {
        try {
            long amountInNanoton = (long) (amountUsdt * 1_000_000);

            String encodedMemo = URLEncoder.encode(memo, StandardCharsets.UTF_8);

            return String.format(
                    "ton://transfer/%s?amount=%d&text=%s&jetton=%s",
                    walletAddress,
                    amountInNanoton,
                    encodedMemo,
                    usdtMasterAddress
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payment link", e);
        }
    }

    private String generateQRCode(String text) {
        try {
            int width = 360;
            int height = 360;

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    width,
                    height
            );

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            byte[] pngData = pngOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public boolean checkTransactionInBlockchain(Payment payment) {
        try {
            String url = String.format("https://tonapi.io/v2/accounts/%s/events?limit=50",
                    walletAddress);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("TON API error: {}", response.statusCode());
                return false;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode events = root.path("events");
            String expectedMemo = payment.getTransactionToken();
            BigDecimal expectedAmount = payment.getAmount();

            for (JsonNode event : events) {
                JsonNode actions = event.path("actions");

                for (JsonNode action : actions) {
                    String actionType = action.path("type").asText();

                    if ("JettonTransfer".equals(actionType)) {
                        JsonNode jettonTransfer = action.path("JettonTransfer");

                        String comment = jettonTransfer.path("comment").asText();
                        String amount = jettonTransfer.path("amount").asText();

                        BigDecimal amountInUsdt = new BigDecimal(amount)
                                .divide(new BigDecimal("1000000"));

                        log.debug("Found transaction - memo: {}, amount: {} USDT",
                                comment, amountInUsdt);

                        if (comment != null && comment.equals(expectedMemo)) {
                            if (amountInUsdt.compareTo(expectedAmount) >= 0) {
                                return true;
                            } else {
                                log.warn("⚠️ Amount mismatch! Blockchain: {} USDT, DB: {} USDT",
                                        amountInUsdt, expectedAmount);
                            }
                        }
                    }
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }
}