package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.PlategaCreateRequest;
import io.nesvpn.telegrambot.dto.PlategaCreateResponse;
import io.nesvpn.telegrambot.dto.PlategaTransactionResponse;
import io.nesvpn.telegrambot.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlategaService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${platega.base-url}")
    private String baseUrl;

    @Value("${platega.merchant-id}")
    private String merchantId;

    @Value("${platega.secret}")
    private String secret;

    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.support}")
    private String supportUsername;

    public PlategaCreateResponse createTransaction(
            double amount,
            String currency,
            String description,
            String payload
    ) {

        String url = baseUrl + "/transaction/process";

        PlategaCreateRequest request = new PlategaCreateRequest();
        request.setPaymentMethod(2);

        PlategaCreateRequest.PaymentDetails details =
                new PlategaCreateRequest.PaymentDetails();
        details.setAmount(amount);
        details.setCurrency(currency);

        request.setPaymentDetails(details);
        request.setDescription(description);
        request.setPayload(payload);

        request.setReturnUrl("https://t.me/" + botUsername);
        request.setFailedUrl("https://t.me/" + supportUsername);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-MerchantId", merchantId);
        headers.set("X-Secret", secret);

        HttpEntity<PlategaCreateRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<PlategaCreateResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        PlategaCreateResponse.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Platega Error: " + response.getStatusCode().toString());
        }

        return response.getBody();
    }

    public boolean checkPayment(Payment payment) {

        String url = baseUrl + "/transaction/" + payment.getTransactionToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MerchantId", merchantId);
        headers.set("X-Secret", secret);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PlategaTransactionResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        PlategaTransactionResponse.class
                );

        return "CONFIRMED".equals(response.getBody().getStatus());
    }
}