package io.nesvpn.subscribelinkservice;

import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.exception.UserNotFoundException;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import io.nesvpn.subscribelinkservice.service.LinkService;
import io.nesvpn.subscribelinkservice.service.NewTokenService;
import io.nesvpn.subscribelinkservice.service.UpdateTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private NewTokenService newTokenService;
    @Mock private UpdateTokenService updateTokenService;

    @InjectMocks private LinkService linkService;

    private User user;
    private Token token;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = User.builder().id(userId).build();
        token = Token.builder().user(user).build();
    }

    @Test
    void process_ShouldCreateNewToken_WhenNoTokenExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(newTokenService.process(userId)).thenReturn(Mono.just("new-link"));
        String result = linkService.process(userId, 1L, 1L).block();
        assertEquals("new-link", result);
        verify(newTokenService).process(userId);
        verify(updateTokenService, never()).process(any(), anyLong(), anyLong());
    }

    @Test
    void process_ShouldUpdateToken_WhenTokenExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(updateTokenService.process(userId, 1L, 1L)).thenReturn(Mono.just("updated-link"));
        String result = linkService.process(userId, 1L, 1L).block();
        assertEquals("updated-link", result);
        verify(updateTokenService).process(userId, 1L, 1L);
        verify(newTokenService, never()).process(any());
    }

    @Test
    void process_ShouldHandleUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> linkService.process(userId, 1L, 1L).block());
    }
}
