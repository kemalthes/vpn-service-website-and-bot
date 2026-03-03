package io.nesvpn.subscribelinkservice;

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
import io.nesvpn.subscribelinkservice.service.UpdateTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateTokenServiceTest {

    @Mock private RemnawaveClient remnawaveClient;
    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private VpnPlanRepository vpnPlanRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private UpdateTokenService updateTokenService;

    private User user;
    private Token token;
    private Order order;
    private VpnPlan plan;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = User.builder().id(userId).build();
        token = Token.builder()
                .user(user)
                .token("old-link")
                .vpnPanelUserUuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
                .validTo(LocalDateTime.now())
                .build();
        order = Order.builder().id(1L).status(OrderStatus.PAID).build();
        plan = VpnPlan.builder().id(1L).duration(30).build();
    }

    @Test
    void process_ShouldUpdateToken_WhenOrderPaid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(vpnPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(remnawaveClient.updateVpnUser(anyString(), eq(0L), any(), any()))
                .thenReturn(Mono.empty());

        String result = updateTokenService.process(userId, 1L, 1L).block();

        assertEquals("old-link", result);
        assertTrue(token.getValidTo().isAfter(LocalDateTime.now()));
        verify(orderRepository).save(argThat(o -> o.getStatus().equals("provided")));
    }

    @Test
    void process_ShouldThrowIdempotentException_WhenOrderNotPaid_ForIdempotency() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> updateTokenService.process(userId, 1L, 1L).block());
        assertThat(exception)
                .hasMessage("Token update failed")
                .hasCauseInstanceOf(IdempotentException.class);
        verify(remnawaveClient, never()).updateVpnUser(anyString(), anyLong(), any(), anyInt());
        verifyNoInteractions(userRepository, tokenRepository, vpnPlanRepository);
    }


}
