package io.nesvpn.subscribelinkservice.service;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.entity.VpnPlan;
import io.nesvpn.subscribelinkservice.enums.OrderOperationType;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.repository.OrderRepository;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateTokenService {

    private final OrderRepository orderRepository;
    private final UtilService utilService;

    private final RemnawaveClient remnawaveClient;
    private final TokenRepository tokenRepository;

    @Transactional
    public Mono<String> process(Long orderId, String tgUsername) {
        log.info("[UpdateTokenService] Старт продления подписки. orderId: {}", orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> {
                    log.error("[UpdateTokenService] Заказ {} не найден!", orderId);
                    return new RuntimeException("Order not found");
                });

        if (order.getStatus() == OrderStatus.PROVIDED) {
            log.info("[UpdateTokenService] Заказ {} уже выполнен. Завершаем без retry.", orderId);
            return Mono.just("");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            log.info("[UpdateTokenService] Заказ {} имеет статус {}. Завершаем без retry.", orderId, order.getStatus());
            return Mono.just("");
        }
        if (order.getVpnPlan() == null) {
            throw new RuntimeException("Plan not found in order");
        }

        User user = order.getUser();
        Token token = order.getToken() != null
                ? tokenRepository.findByIdForUpdate(order.getToken().getId())
                        .orElseThrow(() -> new RuntimeException("Token not found"))
                : tokenRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("Token not found"));
        VpnPlan plan = order.getVpnPlan();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baseValidTo = token.getValidTo() == null || token.getValidTo().isBefore(now) ? now : token.getValidTo();
        LocalDateTime expiresAt = baseValidTo.plusDays(plan.getDuration());
        Integer targetMaxDevices = resolveTargetMaxDevices(order, token, plan);

        log.info("[UpdateTokenService] Продлеваем: старая дата {}, новая дата {}, maxDevices {}",
                token.getValidTo(), expiresAt, targetMaxDevices);
        remnawaveClient.updateVpnUser(
                        token.getVpnPanelUserUuid().toString(),
                        0L,
                        expiresAt,
                        targetMaxDevices,
                        utilService.createDescription(user, tgUsername))
                .block(Duration.ofSeconds(10));

        token.setValidTo(expiresAt);
        token.setMaxDevices(targetMaxDevices);
        if (shouldClearRenewalTarget(order)) {
            token.setRenewalTargetMaxDevices(null);
        }
        tokenRepository.save(token);

        order.setStatus(OrderStatus.PROVIDED);
        orderRepository.save(order);
        log.info("[UpdateTokenService] Заказ {} успешно закрыт, статус изменен на PROVIDED!", orderId);
        return Mono.just(token.getToken());
    }

    private Integer resolveTargetMaxDevices(Order order, Token token, VpnPlan plan) {
        if (order.getTargetMaxDevices() != null) {
            return order.getTargetMaxDevices();
        }
        if (token.getMaxDevices() != null) {
            return token.getMaxDevices();
        }
        if (plan.getDefaultDevices() != null) {
            return plan.getDefaultDevices();
        }
        return 3;
    }

    private boolean shouldClearRenewalTarget(Order order) {
        return OrderOperationType.SUBSCRIPTION_RENEWAL.name().equals(order.getOperationType())
                || OrderOperationType.AUTO_RENEWAL.name().equals(order.getOperationType());
    }
}
