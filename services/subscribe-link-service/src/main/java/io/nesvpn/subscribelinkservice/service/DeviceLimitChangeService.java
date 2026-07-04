package io.nesvpn.subscribelinkservice.service;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.repository.OrderRepository;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceLimitChangeService {

    private final OrderRepository orderRepository;
    private final TokenRepository tokenRepository;
    private final RemnawaveClient remnawaveClient;
    private final UtilService utilService;

    @Transactional
    public Mono<Void> process(Long orderId, String tgUsername) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() == OrderStatus.PROVIDED) {
            log.info("[DeviceLimitChangeService] Заказ {} уже выполнен. Завершаем без retry.", orderId);
            return Mono.empty();
        }
        if (order.getStatus() != OrderStatus.PAID) {
            log.info("[DeviceLimitChangeService] Заказ {} имеет статус {}. Завершаем без retry.", orderId, order.getStatus());
            return Mono.empty();
        }
        if (order.getToken() == null) {
            throw new RuntimeException("Token not found in order");
        }

        Token token = tokenRepository.findByIdForUpdate(order.getToken().getId())
                .orElseThrow(() -> new RuntimeException("Token not found"));
        User user = token.getUser();
        remnawaveClient.updateVpnUser(
                        token.getVpnPanelUserUuid().toString(),
                        0L,
                        token.getValidTo(),
                        order.getTargetMaxDevices(),
                        utilService.createDescription(user, tgUsername))
                .block(Duration.ofSeconds(10));

        token.setMaxDevices(order.getTargetMaxDevices());
        token.setRenewalTargetMaxDevices(null);
        tokenRepository.save(token);

        order.setStatus(OrderStatus.PROVIDED);
        orderRepository.save(order);
        return Mono.empty();
    }
}
