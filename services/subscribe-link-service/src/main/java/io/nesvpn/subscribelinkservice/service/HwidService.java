package io.nesvpn.subscribelinkservice.service;

import io.nesvpn.subscribelinkservice.client.RemnawaveClient;
import io.nesvpn.subscribelinkservice.dto.HwidGetAllResponse;
import io.nesvpn.subscribelinkservice.entity.Token;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.exception.UserNotFoundException;
import io.nesvpn.subscribelinkservice.rabbit.HwidMethod;
import io.nesvpn.subscribelinkservice.rabbit.HwidRequest;
import io.nesvpn.subscribelinkservice.rabbit.HwidResponse;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HwidService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final RemnawaveClient remnawaveClient;

    public HwidResponse process(HwidRequest request) {
        if (request.getMethod() == HwidMethod.DELETE) {
            log.info("[HwidService] запрос на удаление принят");
            return deleteHwidDevice(request);
        }
        return getHwidDevices(request);
    }

    private HwidResponse getHwidDevices(HwidRequest request) {
        try {
            User user = userRepository.findById(request.getUserId()).orElseThrow(UserNotFoundException::new);
            Optional<Token> tokenOpt = tokenRepository.findByUser(user);
            if (tokenOpt.isEmpty()) {
                return HwidResponse.builder()
                        .userId(request.getUserId())
                        .success(false)
                        .message("Token not found")
                        .build();
            }
            String userUuid = tokenOpt.get().getVpnPanelUserUuid().toString();
            HwidGetAllResponse resp = remnawaveClient.getHwidDevices(userUuid)
                    .block(Duration.ofSeconds(10));
            if (resp == null || resp.getResponse() == null) {
                return HwidResponse.builder()
                        .userId(request.getUserId())
                        .success(false)
                        .message("Empty response")
                        .build();
            }
            return HwidResponse.builder()
                    .userId(request.getUserId())
                    .method(request.getMethod())
                    .success(true)
                    .total(resp.getResponse().getTotal())
                    .devices(resp.getResponse().getDevices())
                    .build();
        } catch (Exception ex) {
            log.error("[HwidService] get devices failed: {}", ex.getMessage(), ex);
            return HwidResponse.builder()
                    .userId(request.getUserId())
                    .method(request.getMethod())
                    .success(false)
                    .message(ex.getMessage())
                    .build();
        }
    }

    private HwidResponse deleteHwidDevice(HwidRequest request) {
        if (request.getHwid() == null || request.getHwid().isBlank()) {
            return HwidResponse.builder()
                    .userId(request.getUserId())
                    .method(request.getMethod())
                    .success(false)
                    .message("HWID is empty")
                    .build();
        }
        try {
            User user = userRepository.findById(request.getUserId()).orElseThrow(UserNotFoundException::new);
            Optional<Token> tokenOpt = tokenRepository.findByUser(user);
            if (tokenOpt.isEmpty()) {
                return HwidResponse.builder()
                        .userId(request.getUserId())
                        .method(request.getMethod())
                        .success(false)
                        .message("Token not found")
                        .build();
            }
            Token token = tokenOpt.get();
            String userUuid = token.getVpnPanelUserUuid().toString();
            remnawaveClient.deleteHwidDevice(userUuid, request.getHwid())
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofMinutes(1));
            remnawaveClient.refreshSubscriptionPassword(userUuid)
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofMinutes(1));
            return HwidResponse.builder()
                    .userId(request.getUserId())
                    .method(request.getMethod())
                    .success(true)
                    .hwid(request.getHwid())
                    .message("HWID deleted and subscription updated")
                    .build();
        } catch (Exception ex) {
            log.error("[HwidService] delete device failed: {}", ex.getMessage(), ex);
            return HwidResponse.builder()
                    .userId(request.getUserId())
                    .method(request.getMethod())
                    .success(false)
                    .message(ex.getMessage())
                    .build();
        }
    }
}
