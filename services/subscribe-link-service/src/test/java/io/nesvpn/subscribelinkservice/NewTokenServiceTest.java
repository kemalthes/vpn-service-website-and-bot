package io.nesvpn.subscribelinkservice;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.entity.Order;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.entity.VpnPlan;
import io.nesvpn.subscribelinkservice.enums.OrderStatus;
import io.nesvpn.subscribelinkservice.repository.OrderRepository;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.service.NewTokenService;
import io.nesvpn.subscribelinkservice.service.UtilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewTokenServiceTest {

    @Mock private RemnawaveClient remnawaveClient;
    @Mock private OrderRepository orderRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private UtilService utilService;

    @InjectMocks private NewTokenService newTokenService;

    private User testUser;
    private VpnPlan plan;
    private Order order;
    private final Long orderId = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(newTokenService, "trafficLimit", 0L);
        ReflectionTestUtils.setField(newTokenService, "defaultSquadUuid", "squad-uuid");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setTgId(123456L);
        testUser.setEmail("test@example.com");

        plan = VpnPlan.builder()
                .id(1L)
                .duration(30)
                .defaultDevices(3)
                .build();
        order = Order.builder()
                .id(orderId)
                .user(testUser)
                .vpnPlan(plan)
                .status(OrderStatus.PAID)
                .targetMaxDevices(5)
                .build();
    }

    @Test
    void process_ShouldCreateToken_WhenOrderPaid() {
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(utilService.createDescription(testUser, "tester")).thenReturn("description");
        when(remnawaveClient.createNewVpnUser(
                anyString(),
                eq(123456L),
                eq("test@example.com"),
                any(LocalDateTime.class),
                eq(5),
                eq(0L),
                eq("squad-uuid"),
                eq("description")))
                .thenReturn(Mono.just(validUuid));
        when(remnawaveClient.getUserLink(validUuid)).thenReturn(Mono.just("vpn://link-123"));

        String result = newTokenService.process(orderId, "tester").block();

        assertEquals("vpn://link-123", result);
        verify(remnawaveClient).createNewVpnUser(
                anyString(),
                eq(123456L),
                eq("test@example.com"),
                any(LocalDateTime.class),
                eq(5),
                eq(0L),
                eq("squad-uuid"),
                eq("description"));
        verify(tokenRepository).save(argThat(token ->
                token.getUser().equals(testUser)
                        && token.getVpnPanelUserUuid().toString().equals(validUuid)
                        && token.getMaxDevices().equals(5)));
        verify(orderRepository).save(argThat(savedOrder -> savedOrder.getStatus() == OrderStatus.PROVIDED));
    }

    @Test
    void process_ShouldReturnEmpty_WhenOrderAlreadyProvided() {
        order.setStatus(OrderStatus.PROVIDED);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        String result = newTokenService.process(orderId, "tester").block();

        assertEquals("", result);
        verifyNoInteractions(remnawaveClient, tokenRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void process_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> newTokenService.process(orderId, "tester").block());
    }

    @Test
    void process_ShouldPropagateRemnawaveError() {
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(utilService.createDescription(testUser, "tester")).thenReturn("description");
        when(remnawaveClient.createNewVpnUser(
                anyString(),
                anyLong(),
                anyString(),
                any(LocalDateTime.class),
                eq(5),
                eq(0L),
                eq("squad-uuid"),
                eq("description")))
                .thenReturn(Mono.error(new RuntimeException("remote error")));

        assertThrows(RuntimeException.class,
                () -> newTokenService.process(orderId, "tester").block());
    }
}
