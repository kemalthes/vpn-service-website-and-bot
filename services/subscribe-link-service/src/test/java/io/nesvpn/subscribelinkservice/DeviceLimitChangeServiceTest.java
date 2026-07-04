package io.nesvpn.subscribelinkservice;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.repository.OrderRepository;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.service.DeviceLimitChangeService;
import io.nesvpn.subscribelinkservice.service.UtilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceLimitChangeServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private RemnawaveClient remnawaveClient;
    @Mock private UtilService utilService;

    @InjectMocks
    private DeviceLimitChangeService deviceLimitChangeService;

    private User user;
    private Token token;
    private Order order;
    private final Long orderId = 1L;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        token = Token.builder()
                .id(10L)
                .user(user)
                .vpnPanelUserUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
                .validTo(LocalDateTime.now().plusDays(10))
                .maxDevices(5)
                .renewalTargetMaxDevices(3)
                .build();
        order = Order.builder()
                .id(orderId)
                .token(token)
                .status(OrderStatus.PAID)
                .targetMaxDevices(8)
                .build();
    }

    @Test
    void process_ShouldUpdateDeviceLimit_WhenOrderPaid() {
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(tokenRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(token));
        when(utilService.createDescription(user, "tester")).thenReturn("description");
        when(remnawaveClient.updateVpnUser(
                anyString(),
                eq(0L),
                eq(token.getValidTo()),
                eq(8),
                eq("description")))
                .thenReturn(Mono.empty());

        deviceLimitChangeService.process(orderId, "tester").block();

        assertEquals(8, token.getMaxDevices());
        assertEquals(null, token.getRenewalTargetMaxDevices());
        verify(tokenRepository).save(token);
        verify(orderRepository).save(argThat(savedOrder -> savedOrder.getStatus() == OrderStatus.PROVIDED));
    }

    @Test
    void process_ShouldReturnEmpty_WhenOrderAlreadyProvided() {
        order.setStatus(OrderStatus.PROVIDED);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        deviceLimitChangeService.process(orderId, "tester").block();

        verifyNoInteractions(remnawaveClient, tokenRepository);
        verify(orderRepository, never()).save(order);
    }
}
