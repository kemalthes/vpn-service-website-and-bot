package io.nesvpn.subscribelinkservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public WebClient webClient(
            @Value("${remnawave.url}") String baseUrl,
            @Value("${remnawave.key}") String apiKey
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.setBearerAuth(apiKey);
                    httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
