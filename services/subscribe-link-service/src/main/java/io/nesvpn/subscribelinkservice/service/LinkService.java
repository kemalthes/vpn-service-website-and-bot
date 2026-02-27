package io.nesvpn.subscribelinkservice.service;

import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.exception.UserNotFoundException;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class LinkService {

    private final UserRepository userRepository;
    private final NewTokenService newTokenService;
    private final UpdateTokenService updateTokenService;
    private final TokenRepository tokenRepository;

    @Transactional(readOnly = true)
    public Mono<String> process(UUID userId, Long planId, Long orderId) {
        return Mono.fromCallable(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(UserNotFoundException::new);
                    if (tokenRepository.findByUser(user).isEmpty()) {
                        return newTokenService.process(userId, orderId).block();
                    } else {
                        return updateTokenService.process(userId, planId, orderId).block();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
