package io.nesvpn.telegrambot.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class FloatRatesService {

    private static final String FLOAT_RATES_URL = "http://www.floatrates.com/daily/usd.json";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile Double cachedRate = null;
    private volatile LocalDateTime lastUpdateTime = null;
    private static final long CACHE_DURATION_MINUTES = 30;

    private final ReentrantLock lock = new ReentrantLock();

    public FloatRatesService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();

        refreshRateAsync();
    }

    public double getUsdToRubRate() {
        if (cachedRate != null && lastUpdateTime != null &&
                lastUpdateTime.plusMinutes(CACHE_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
            return cachedRate;
        }

        if (cachedRate != null) {
            refreshRateAsync();
            return cachedRate;
        }

        return refreshRateSync();
    }

    private void refreshRateAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        double newRate = fetchRate();
                        cachedRate = newRate;
                        lastUpdateTime = LocalDateTime.now();
                        log.info("Rate updated asynchronously: {}", newRate);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private double refreshRateSync() {
        if (cachedRate != null) {
            return cachedRate;
        }

        double newRate = fetchRate();
        cachedRate = newRate;
        lastUpdateTime = LocalDateTime.now();
        return newRate;
    }

    private double fetchRate() {
        int retries = 3;
        while (retries-- > 0) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(FLOAT_RATES_URL))
                        .timeout(Duration.ofSeconds(3))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode rubNode = root.get("rub");

                    if (rubNode != null) {
                        return rubNode.get("rate").asDouble();
                    }
                }

                Thread.sleep(500);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (cachedRate != null) {
            return cachedRate;
        }

        throw new RuntimeException("Failed to fetch exchange rate after all retries");
    }

    public void forceRefresh() {
        cachedRate = null;
        lastUpdateTime = null;
        refreshRateAsync();
    }
}