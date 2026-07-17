package io.nesvpn.subscribelinkservice;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.entity.VpnPlan;
import io.nesvpn.subscribelinkservice.enums.OrderOperationType;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.exception.UserNotFoundException;
import io.nesvpn.subscribelinkservice.repository.OrderRepository;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import io.nesvpn.subscribelinkservice.service.LinkService;
import io.nesvpn.subscribelinkservice.service.NewTokenService;
import io.nesvpn.subscribelinkservice.service.UpdateTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private NewTokenService newTokenService;
    @Mock private UpdateTokenService updateTokenService;
    @Mock private OrderRepository orderRepository;

    private LinkService linkService;
    private User user;
    private Token token;
    private Order order;
    private VpnPlan plan;
    private final UUID userId = UUID.randomUUID();
    private final Long orderId = 1L;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(
                userRepository,
                newTokenService,
                updateTokenService,
                orderRepository,
                tokenRepository,
                new SimpleMeterRegistry());
        linkService.init();

        user = User.builder().id(userId).build();
        plan = VpnPlan.builder().id(1L).build();
        token = Token.builder().id(10L).user(user).build();
        order = Order.builder()
                .id(orderId)
                .user(user)
                .vpnPlan(plan)
                .status(OrderStatus.PAID)
                .operationType(OrderOperationType.NEW_SUBSCRIPTION.name())
                .build();
    }

    @Test
    void process_ShouldCreateNewToken_WhenNoTokenExists() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(newTokenService.process(orderId, "tester")).thenReturn(Mono.just("new-link"));

        String result = linkService.process(userId, 1L, orderId, "tester").block();

        assertEquals("new-link", result);
        verify(newTokenService).process(orderId, "tester");
        verify(updateTokenService, never()).process(anyLong(), anyString());
    }

    @Test
    void process_ShouldUpdateToken_WhenTokenExists() {
        order.setOperationType(OrderOperationType.SUBSCRIPTION_RENEWAL.name());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(updateTokenService.process(orderId, "tester")).thenReturn(Mono.just("updated-link"));

        String result = linkService.process(userId, 1L, orderId, "tester").block();

        assertEquals("updated-link", result);
        verify(updateTokenService).process(orderId, "tester");
        verify(newTokenService, never()).process(anyLong(), anyString());
    }

    @Test
    void process_ShouldReturnEmpty_WhenOrderAlreadyProvided() {
        order.setStatus(OrderStatus.PROVIDED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        String result = linkService.process(userId, 1L, orderId, "tester").block();

        assertEquals("", result);
        verifyNoInteractions(userRepository, tokenRepository, newTokenService, updateTokenService);
    }

    @Test
    void process_ShouldHandleUserNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> linkService.process(userId, 1L, orderId, "tester").block());
    }
}
