package io.nesvpn.subscribelinkservice.client;

import io.nesvpn.subscribelinkservice.dto.NewUserRequest;
import io.nesvpn.subscribelinkservice.dto.UpdateUserRequest;
import io.nesvpn.subscribelinkservice.exception.GetLinkException;
import io.nesvpn.subscribelinkservice.exception.NewUserCreationException;
import io.nesvpn.subscribelinkservice.exception.UpdateUserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemnawaveClient implements VpnClient {

    private final WebClient webClient;

    @Value("${remnawave.key}")
    private String apiKey;

    @Override
    public Mono<String> createNewVpnUser(String username, Long tgId, String email, LocalDateTime expiredAt, Integer deviceLimit, Long trafficLimit, String squadUuid) {
        NewUserRequest newUserRequest = new NewUserRequest(
                username, tgId, email, new String[]{squadUuid}, trafficLimit, String.valueOf(expiredAt), deviceLimit
        );
        log.info("[Create] Отправляем POST запрос в /api/users для username: {}", username);
        return webClient.post()
                .uri("/api/users")
                .headers(h -> h.addAll(getAuthHeaders()))
                .bodyValue(newUserRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(json -> log.info("📥 [Create] Успешный ответ: {}", json))
                .flatMap(resp -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseBlock = (Map<String, Object>) resp.get("response");
                        if (responseBlock == null || !responseBlock.containsKey("uuid") || responseBlock.get("uuid") == null) {
                            log.error("[Create] Ошибка парсинга: нет поля 'uuid'! Ответ: {}", resp);
                            return Mono.error(new NewUserCreationException());
                        }
                        String uuid = responseBlock.get("uuid").toString();
                        log.info("[Create] Успешно извлечен UUID: {}", uuid);
                        return Mono.just(uuid);
                    } catch (Exception e) {
                        log.error("[Create] Ошибка при чтении Map: {}", e.getMessage());
                        return Mono.error(new NewUserCreationException());
                    }
                })
                .doOnError(WebClientResponseException.class, ex -> log.error("[Create] API вернуло HTTP ошибку! Статус: {}. Ответ: {}",
                        ex.getStatusCode(), ex.getResponseBodyAsString()))
                .doOnError(ex -> {
                    if (!(ex instanceof WebClientResponseException)) {
                        log.error("[Create] Сетевая/локальная ошибка: {}", ex.getMessage());
                    }
                })
                .onErrorMap(ex -> new NewUserCreationException());
    }

    @Override
    public Mono<Void> updateVpnUser(String userUuid, Long dataLimitBytes, LocalDateTime expiresAt, Integer maxDevices) {
        UpdateUserRequest updateBody = new UpdateUserRequest(
                userUuid,
                dataLimitBytes,
                expiresAt.format(DateTimeFormatter.ISO_DATE_TIME),
                maxDevices
        );
        log.info("[Update] Отправляем PATCH запрос для юзера: {}", userUuid);
        return webClient.patch()
                .uri("/api/users/")
                .headers(h -> h.addAll(getAuthHeaders()))
                .bodyValue(updateBody)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(json -> log.info("[Update] Успешно обновлен юзер {}. Ответ: {}", userUuid, json))
                .doOnError(WebClientResponseException.class, ex -> log.error("[Update] API вернуло HTTP ошибку! Статус: {}. Ответ: {}",
                        ex.getStatusCode(), ex.getResponseBodyAsString()))
                .doOnError(ex -> {
                    if (!(ex instanceof WebClientResponseException)) {
                        log.error("[Update] Сетевая/локальная ошибка: {}", ex.getMessage());
                    }
                })
                .onErrorMap(ex -> new UpdateUserException())
                .then();
    }

    @Override
    public Mono<String> getUserLink(String userUuid) {
        log.info("[Link] Запрашиваем короткую ссылку для юзера: {}", userUuid);
        return webClient.get()
                .uri("/api/users/" + userUuid)// Добавил проброс токена!
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(resp -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseBlock = (Map<String, Object>) resp.get("response");
                        if (responseBlock == null || !responseBlock.containsKey("shortUuid") || responseBlock.get("shortUuid") == null) {
                            log.error("[Link] Ошибка парсинга: нет поля 'shortUuid'! Ответ: {}", resp);
                            return Mono.error(new GetLinkException());
                        }
                        String shortUuid = responseBlock.get("shortUuid").toString();
                        log.info("[Link] Успешно извлечен shortUuid: {}", shortUuid);
                        return Mono.just(shortUuid);
                    } catch (Exception e) {
                        log.error("[Link] Ошибка при чтении Map: {}", e.getMessage());
                        return Mono.error(new GetLinkException());
                    }
                })
                .doOnError(WebClientResponseException.class, ex -> log.error("[Link] API вернуло HTTP ошибку! Статус: {}. Ответ: {}",
                        ex.getStatusCode(), ex.getResponseBodyAsString()))
                .doOnError(ex -> {
                    if (!(ex instanceof WebClientResponseException)) {
                        log.error("[Link] Сетевая/локальная ошибка: {}", ex.getMessage());
                    }
                })
                .onErrorMap(ex -> {
                    if (ex instanceof GetLinkException) return ex;
                    return new GetLinkException();
                });
    }

    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}