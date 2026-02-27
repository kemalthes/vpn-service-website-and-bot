package io.nesvpn.subscribelinkservice.client;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface VpnClient {

    Mono<String> createNewVpnUser(String username, Long tgId, String email, LocalDateTime expiredAt, Integer deviceLimit, Long trafficLimit, String squadUuid);

    Mono<Void> updateVpnUser(String userUuid, Long dataLimitBytes, LocalDateTime expiresAt, Integer maxDevices);

    Mono<String> getUserLink(String userUuid);
}
