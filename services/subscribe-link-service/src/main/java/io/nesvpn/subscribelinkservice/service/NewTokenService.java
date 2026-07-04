package io.nesvpn.subscribelinkservice.service;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.entity.VpnPlan;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.enums.TokenStatus;
import io.nesvpn.subscribelinkservice.repository.OrderRepository;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewTokenService {

    private final RemnawaveClient remnawaveClient;
    private final OrderRepository orderRepository;
    private final TokenRepository tokenRepository;
    private final UtilService utilService;

    @Value("#{${project.traffic-limit-free}}")
    private Long trafficLimit;

    @Value("${remnawave.squad-uuid}")
    private String defaultSquadUuid;

    @Transactional
    public Mono<String> process(Long orderId, String tgUsername) {
        log.info("[NewTokenService] Начинаем создание токена по заказу: {}", orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() == OrderStatus.PROVIDED) {
            log.info("[NewTokenService] Заказ {} уже выполнен. Завершаем без retry.", orderId);
            return Mono.just("");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            log.info("[NewTokenService] Заказ {} имеет статус {}. Завершаем без retry.", orderId, order.getStatus());
            return Mono.just("");
        }
        if (order.getVpnPlan() == null) {
            throw new RuntimeException("Plan not found in order");
        }

        User user = order.getUser();
        VpnPlan plan = order.getVpnPlan();
        String username = "user_".concat(String.valueOf(orderId));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(plan.getDuration());
        Integer maxDevices = order.getTargetMaxDevices() != null
                ? order.getTargetMaxDevices()
                : plan.getDefaultDevices() != null ? plan.getDefaultDevices() : 3;

        log.info("[NewTokenService] Отправляем запрос в Remnawave (createNewVpnUser). Username: {}, tgId: {}", username, user.getTgId());
        String vpnUserUuid = remnawaveClient.createNewVpnUser(
                        username,
                        user.getTgId(),
                        user.getEmail(),
                        expiresAt,
                        maxDevices,
                        trafficLimit,
                        defaultSquadUuid,
                        utilService.createDescription(user, tgUsername))
                .block(Duration.ofSeconds(10));
        log.info("[NewTokenService] Успешный ответ от Remnawave! Получен VPN User UUID: {}", vpnUserUuid);

        String vpnUrl = remnawaveClient.getUserLink(vpnUserUuid)
                .block(Duration.ofSeconds(10));
        Token token = Token.builder()
                .user(user)
                .token(vpnUrl)
                .createdAt(now)
                .status(TokenStatus.ACTIVE)
                .validTo(expiresAt)
                .vpnPanelUserUuid(java.util.UUID.fromString(Objects.requireNonNull(vpnUserUuid)))
                .maxDevices(maxDevices)
                .renewalTargetMaxDevices(null)
                .build();
        tokenRepository.save(token);

        order.setStatus(OrderStatus.PROVIDED);
        orderRepository.save(order);
        log.info("[NewTokenService] Токен успешно сохранен в БД для пользователя: {}", user.getId());
        return Mono.just(vpnUrl);
    }

}
