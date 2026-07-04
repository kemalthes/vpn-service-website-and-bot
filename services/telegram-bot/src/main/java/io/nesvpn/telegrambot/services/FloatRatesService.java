package io.nesvpn.telegrambot.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private static final int RATE_FETCH_RETRIES = 3;
    private static final long CACHE_DURATION_MINUTES = 30;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${project.fallback-usd-to-rub-rate:90}")
    private double fallbackUsdToRubRate;

    private volatile Double cachedRate = null;
    private volatile LocalDateTime lastUpdateTime = null;

    private final ReentrantLock lock = new ReentrantLock();

    public FloatRatesService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
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
            boolean locked = false;
            try {
                locked = lock.tryLock(1, TimeUnit.SECONDS);
                if (locked) {
                    double newRate = fetchRate();
                    cacheRate(newRate);
                    log.info("USD/RUB rate updated asynchronously: {}", newRate);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("USD/RUB rate refresh was interrupted. Using rate: {}", getCachedOrFallbackRate());
            } catch (Exception e) {
                log.warn("Failed to refresh USD/RUB rate: {}. Using rate: {}", e.getMessage(), getCachedOrFallbackRate());
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
        });
    }

    private double refreshRateSync() {
        if (cachedRate != null) {
            return cachedRate;
        }

        try {
            double newRate = fetchRate();
            cacheRate(newRate);
            return newRate;
        } catch (Exception e) {
            double fallbackRate = getCachedOrFallbackRate();
            cacheRate(fallbackRate);
            log.warn("Failed to fetch USD/RUB rate: {}. Using fallback rate: {}", e.getMessage(), fallbackRate);
            return fallbackRate;
        }
    }

    private double fetchRate() {
        int retries = RATE_FETCH_RETRIES;
        Exception lastException = null;
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
                    lastException = new IllegalStateException("RUB rate is missing in FloatRates response");
                } else {
                    lastException = new IllegalStateException("FloatRates returned HTTP " + response.statusCode());
                }

                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while fetching USD/RUB rate", e);
            } catch (Exception e) {
                lastException = e;
            }
        }

        if (cachedRate != null) {
            return cachedRate;
        }

        throw new IllegalStateException(
                "Failed to fetch USD/RUB rate after " + RATE_FETCH_RETRIES + " retries",
                lastException
        );
    }

    private void cacheRate(double rate) {
        cachedRate = rate;
        lastUpdateTime = LocalDateTime.now();
    }

    private double getCachedOrFallbackRate() {
        return cachedRate != null ? cachedRate : fallbackUsdToRubRate;
    }

    public void forceRefresh() {
        cachedRate = null;
        lastUpdateTime = null;
        refreshRateAsync();
    }
}
