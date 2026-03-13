package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.HwidDevice;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.rabbit.HwidMethod;
import io.nesvpn.telegrambot.rabbit.HwidRabbitProducer;
import io.nesvpn.telegrambot.rabbit.HwidRequest;
import io.nesvpn.telegrambot.rabbit.HwidResponse;
import io.nesvpn.telegrambot.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {
    private final HwidRabbitProducer hwidRabbitProducer;

    @Value("${project.vpn-host-url}")
    private String vpnHostUrl;

    @Value("${project.vpn-panel-token}")
    private String bearerToken;

    @Value("${project.vpn-panel-url}")
    private String vpnPanelUrl;

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

        long hours = ChronoUnit.HOURS.between(now, token.getValidTo());

        return Math.max(1, Math.round(hours / 24.0));
    }

    public List<HwidDevice> getHwidDevicesByToken(UUID userUuid) {
        HwidResponse response = hwidRabbitProducer.sendHwidRequest(HwidRequest.builder()
                .userId(userUuid)
                .method(HwidMethod.GET_ALL)
                .build());
        return response.getDevices();
    }

    public boolean deleteHwidDeviceByToken(UUID userUuid, String hwid) {
        HwidResponse response = hwidRabbitProducer.sendHwidRequest(HwidRequest.builder()
                .userId(userUuid)
                .method(HwidMethod.DELETE)
                .hwid(hwid)
                .build());
        return response.isSuccess();
    }

    public List<Token> getExpiringTokens(LocalDateTime now, LocalDateTime twoDaysLater) {
        return tokenRepository.findExpiringTokens(now, twoDaysLater);
    }

    public Optional<Token> findById(Long id) {
        return tokenRepository.findById(id);
    }

    public String getFullTokenUrl(Token token) {
        return vpnHostUrl + token.getToken();
    }
}
