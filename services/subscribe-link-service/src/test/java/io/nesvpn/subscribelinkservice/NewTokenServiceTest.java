package io.nesvpn.subscribelinkservice;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import io.nesvpn.subscribelinkservice.service.NewTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewTokenServiceTest {

    @Mock private RemnawaveClient remnawaveClient;
    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;

    @InjectMocks private NewTokenService newTokenService;

    private User testUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setTgId(123456L);
        testUser.setEmail("test@example.com");
    }

    @Test
    void process_ShouldCreateToken_WhenUserExists() {
        String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";  // ✅ Правильный формат
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(remnawaveClient.createNewVpnUser(anyString(), eq(123456L), eq("test@example.com")))
                .thenReturn(Mono.just(VALID_UUID));  // ✅ Валидный UUID
        when(remnawaveClient.updateVpnUser(eq(VALID_UUID), anyLong(), any(), eq(3)))
                .thenReturn(Mono.empty());
        when(remnawaveClient.getUserLink(eq(VALID_UUID)))
                .thenReturn(Mono.just("vpn://link-123"));
        String result = newTokenService.process(userId).block();
        assertEquals("vpn://link-123", result);
        verify(remnawaveClient).createNewVpnUser(startsWith("user_"), eq(123456L), eq("test@example.com"));
        verify(tokenRepository).save(argThat(token ->
                token.getVpnPanelUserUuid().toString().equals(VALID_UUID)));  // ✅ Совпадает
    }


    @Test
    void process_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> newTokenService.process(userId).block());
        assertTrue(exception.getMessage().contains("Token creation failed"));
    }

    @Test
    void process_ShouldHandleWebClientTimeout() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(remnawaveClient.createNewVpnUser(anyString(), anyLong(), anyString()))
                .thenReturn(Mono.delay(Duration.ofSeconds(3)).then(Mono.just("uuid")));
        assertThrows(RuntimeException.class, () ->
                newTokenService.process(userId).block(Duration.ofSeconds(1)));
    }
}
