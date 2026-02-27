package io.nesvpn.subscribelinkservice.service;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.entity.VpnPlan;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.exception.IdempotentException;
import io.nesvpn.subscribelinkservice.repository.OrderRepository;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import io.nesvpn.subscribelinkservice.repository.VpnPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateTokenService {

    private final OrderRepository orderRepository;

    @Value("${project.limit-devices}")
    private Integer limitDevices;

    private final UserRepository userRepository;
    private final RemnawaveClient remnawaveClient;
    private final TokenRepository tokenRepository;
    private final VpnPlanRepository vpnPlanRepository;

    @Transactional
    public Mono<String> process(UUID userId, Long planId, Long orderId) {
        log.info("[UpdateTokenService] Старт продления подписки. userId: {}, orderId: {}, planId: {}", userId, orderId, planId);
        return Mono.fromCallable(() -> {
                    log.debug("[UpdateTokenService] Ищем заказ {} в БД...", orderId);
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> {
                                log.error("[UpdateTokenService] Заказ {} не найден!", orderId);
                                return new RuntimeException("Order not found");
                            });

                    if (!order.getStatus().equals(OrderStatus.PAID)) {
                        log.warn("[UpdateTokenService] Заказ {} имеет статус {}. Ожидался PAID. Пропускаем обработку (сработала идемпотентность)!", orderId, order.getStatus());
                        throw new IdempotentException();
                    }
                    log.debug("[UpdateTokenService] Ищем пользователя {}, его токен и выбранный тариф {}...", userId, planId);
                    User user = userRepository.findById(userId).orElseThrow(() -> {
                        log.error("[UpdateTokenService] Пользователь {} не найден!", userId);
                        return new RuntimeException("User not found");
                    });
                    Token token = tokenRepository.findByUser(user)
                            .orElseThrow(() -> {
                                log.error("[UpdateTokenService] Токен для пользователя {} не найден!", userId);
                                return new RuntimeException("Token not found");
                            });
                    VpnPlan plan = vpnPlanRepository.findById(planId).orElseThrow(() -> {
                        log.error("[UpdateTokenService] Тариф {} не найден!", planId);
                        return new RuntimeException("Plan not found");
                    });

                    LocalDateTime expiresAt = token.getValidTo().plusDays(plan.getDuration());
                    log.info("[UpdateTokenService] Продлеваем: старая дата {}, новая дата {}", token.getValidTo(), expiresAt);

                    log.info("[UpdateTokenService] Отправляем PATCH запрос в Remnawave для UUID: {}", token.getVpnPanelUserUuid());
                    remnawaveClient.updateVpnUser(
                                    token.getVpnPanelUserUuid().toString(), 0L, expiresAt, limitDevices)
                            .block(Duration.ofSeconds(10));
                    log.info("[UpdateTokenService] Успешный ответ от Remnawave при обновлении дат!");
                    log.debug("[UpdateTokenService] Сохраняем новую дату токена и закрываем заказ в БД...");
                    token.setValidTo(expiresAt);
                    tokenRepository.save(token);
                    order.setStatus(OrderStatus.PROVIDED);
                    orderRepository.save(order);
                    log.info("[UpdateTokenService] Заказ {} успешно закрыт, статус изменен на PROVIDED!", orderId);
                    return token.getToken();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> {
                    if (ex instanceof IdempotentException) {
                        log.info("[UpdateTokenService] Процесс прерван безопасно: заказ уже был выполнен ранее.");
                    } else {
                        log.error("[UpdateTokenService] Критическая ошибка при обновлении заказа {}. Причина: {}", orderId, ex.getMessage(), ex);
                    }
                })
                .onErrorMap(ex -> {
                    if (ex instanceof IdempotentException) {
                        return ex;
                    }
                    return new RuntimeException("Token update failed", ex);
                });
    }
}