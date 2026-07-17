package io.nesvpn.subscribelinkservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.enums.OrderOperationType;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.exception.UserNotFoundException;
import io.nesvpn.subscribelinkservice.repository.OrderRepository;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LinkService {

    private final UserRepository userRepository;
    private final NewTokenService newTokenService;
    private final UpdateTokenService updateTokenService;
    private final OrderRepository orderRepository;
    private final TokenRepository tokenRepository;
    private final MeterRegistry meterRegistry;

    private Counter newTokenCounter;
    private Counter updateTokenCounterOne;
    private Counter updateTokenCounterThree;
    private Counter updateTokenCounterSix;

    @PostConstruct
    public void init() {
        newTokenCounter = Counter.builder("bot_actions")
                .description("Действия с ссылками")
                .tag("action", "newToken")
                .register(this.meterRegistry);
        updateTokenCounterOne = Counter.builder("bot_actions")
                .description("Действия с ссылками")
                .tag("action", "updateToken-1")
                .register(this.meterRegistry);
        updateTokenCounterThree = Counter.builder("bot_actions")
                .description("Действия с ссылками")
                .tag("action", "updateToken-3")
                .register(this.meterRegistry);
        updateTokenCounterSix = Counter.builder("bot_actions")
                .description("Действия с ссылками")
                .tag("action", "updateToken-6")
                .register(this.meterRegistry);
    }

    @Transactional
    public Mono<String> process(UUID userId, Long planId, Long orderId, String tgUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() == OrderStatus.PROVIDED) {
            return Mono.just("");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            return Mono.just("");
        }
        if (OrderOperationType.DEVICE_LIMIT_CHANGE.name().equals(order.getOperationType())) {
            return Mono.just("");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        boolean hasToken = order.getToken() != null || tokenRepository.findByUser(user).isPresent();
        if (!hasToken) {
            newTokenCounter.increment();
            return newTokenService.process(orderId, tgUsername);
        }

        Long effectivePlanId = order.getVpnPlan() != null ? order.getVpnPlan().getId() : planId;
        if (effectivePlanId != null) {
            switch (effectivePlanId.intValue()) {
                case 1:
                    updateTokenCounterOne.increment();
                    break;
                case 2:
                    updateTokenCounterThree.increment();
                    break;
                case 3:
                    updateTokenCounterSix.increment();
                    break;
            }
        }
        return updateTokenService.process(orderId, tgUsername);
    }
}
