package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    @Value("${project.vpn-host-url}")
    private String vpnHostUrl;

    private final TokenRepository tokenRepository;

    public Token getUserToken(UUID userId) {
        return tokenRepository.findByUserId(userId).orElse(null);
    }

    public long getDaysLeft(Token token) {
        if (token == null || token.getValidTo() == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (token.getValidTo().isBefore(now)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(now, token.getValidTo());
    }

    public Optional<Token> findById(Long id) {
        return tokenRepository.findById(id);
    }

    public String getFullTokenUrl(Token token) {
        return vpnHostUrl + token.getToken();
    }
}
