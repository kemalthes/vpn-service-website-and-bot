package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.HwidApiResponse;
import io.nesvpn.telegrambot.dto.HwidDeleteRequest;
import io.nesvpn.telegrambot.dto.HwidDevice;
import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {
    private final RestTemplate restTemplate = new RestTemplate();

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

    public List<HwidDevice> getHwidDevicesByToken(Token token) {
        if (token.getValidTo().isBefore(LocalDateTime.now())) {
            return null;
        }

        UUID userVpnPanelUuid = token.getVpnPanelUserUuid();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearerToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = vpnPanelUrl + String.format("/api/hwid/devices/%s", userVpnPanelUuid);

        ResponseEntity<HwidApiResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        HwidApiResponse.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Get HWID Devices Error status: {}, body: {} ",
                    response.getStatusCode(),
                    response.getBody());
        }

        HwidApiResponse body = response.getBody();

        if (body != null) {
            HwidApiResponse.Response inner = body.getResponse();
            return inner.getDevices();
        }

        return null;
    }

    public boolean deleteHwidDeviceByToken(Token token, String hwid) {
        if (token.getValidTo().isBefore(LocalDateTime.now())) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearerToken);

        String url = vpnPanelUrl + "/api/hwid/devices/delete";
        UUID userVpnPanelUuid = token.getVpnPanelUserUuid();


        HwidDeleteRequest request = new HwidDeleteRequest(userVpnPanelUuid, hwid);
        HttpEntity<HwidDeleteRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<HwidApiResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            HwidApiResponse.class
                    );

            return response.getStatusCode().is2xxSuccessful() && refreshSubscriptionPasswords(userVpnPanelUuid);

        } catch (RestClientException e) {
            log.error("Delete HWID Device request failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean refreshSubscriptionPasswords(UUID vpnPanelUserUuid) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearerToken);

        Map<String, Object> body = new HashMap<>();
        body.put("revokeOnlyPasswords", true);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = vpnPanelUrl + "/api/users/" + vpnPanelUserUuid + "/actions/revoke";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (RestClientException e) {
            log.error("Revoke subscription request failed: {}", e.getMessage(), e);
            return false;
        }
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
