package io.nesvpn.subscribelinkservice.service;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.enums.TokenStatus;
import io.nesvpn.subscribelinkservice.exception.IdempotentException;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j // Добавили поддержку логов
@Service
@RequiredArgsConstructor
public class NewTokenService {

    private final RemnawaveClient remnawaveClient;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${project.limit-devices-free}")
    private Integer limitDevices;

    @Value("#{${project.traffic-limit-free}}")
    private Long trafficLimit;

    @Value("${remnawave.squad-uuid}")
    private String defaultSquadUuid;

    @Value("${project.days-limit}")
    private Integer daysLimit;

    @Transactional
    public Mono<String> process(UUID userId, Long orderId) {
        log.info("[NewTokenService] Начинаем создание токена для пользователя: {}", userId);
        return Mono.fromCallable(() -> {
                    log.debug("[NewTokenService] Поиск пользователя {} в базе данных...", userId);
                    User user = userRepository.findById(userId).orElseThrow(() -> {
                        log.error("[NewTokenService] Пользователь {} не найден в БД!", userId);
                        return new RuntimeException("User not found");
                    });
                    String username = "user_".concat(String.valueOf(orderId));
                    LocalDateTime now = LocalDateTime.now();
                    log.info("[NewTokenService] Отправляем запрос в Remnawave (createNewVpnUser). Username: {}, tgId: {}", username, user.getTgId());
                    String vpnUserUuid = remnawaveClient.createNewVpnUser(
                                    username, user.getTgId(), user.getEmail(), now.plusDays(daysLimit), limitDevices, trafficLimit, defaultSquadUuid)
                            .block(Duration.ofSeconds(10));
                    log.info("[NewTokenService] Успешный ответ от Remnawave! Получен VPN User UUID: {}", vpnUserUuid);
                    log.info("[NewTokenService] Запрашиваем ссылку (getUserLink) для UUID: {}", vpnUserUuid);
                    String vpnUrl = remnawaveClient.getUserLink(vpnUserUuid)
                            .block(Duration.ofSeconds(10));
                    log.info("[NewTokenService] Успешно получена VPN ссылка: {}", vpnUrl);
                    Token token = Token.builder()
                            .user(user)
                            .token(vpnUrl)
                            .createdAt(now)
                            .status(TokenStatus.ACTIVE)
                            .validTo(now.plusDays(3))
                            .vpnPanelUserUuid(UUID.fromString(Objects.requireNonNull(vpnUserUuid)))
                            .build();
                    log.debug("[NewTokenService] Сохраняем токен в базу данных...");
                    tokenRepository.save(token);
                    log.info("[NewTokenService] Токен успешно сохранен в БД для пользователя: {}", userId);
                    return vpnUrl;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("[NewTokenService] Критическая ошибка в процессе создания токена для юзера {}. Причина: {}", userId, ex.getMessage(), ex))
                .onErrorMap(ex -> new RuntimeException("Token creation failed", ex));
    }
}