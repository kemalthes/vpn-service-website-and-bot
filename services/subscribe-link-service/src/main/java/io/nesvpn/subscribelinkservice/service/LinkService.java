package io.nesvpn.subscribelinkservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.nesvpn.subscribelinkservice.entity.User;
import io.nesvpn.subscribelinkservice.exception.UserNotFoundException;
import io.nesvpn.subscribelinkservice.repository.TokenRepository;
import io.nesvpn.subscribelinkservice.repository.UserRepository;
import jakarta.annotation.PostConstruct;
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
    private final MeterRegistry meterRegistry;

    private Counter newTokenCounter;
    private Counter updateTokenCounterOne;
    private Counter updateTokenCounterThree;
    private Counter updateTokenCounterSix;

    @PostConstruct
    public void init() {
        newTokenCounter = Counter.builder("bot_actions")
                .description("Действия с ссылками")
                .tag("action", "newToken")
                .register(this.meterRegistry);
        updateTokenCounterOne = Counter.builder("bot_actions")
                .description("Действия с ссылками")
                .tag("action", "updateToken-1")
                .register(this.meterRegistry);
        updateTokenCounterThree = Counter.builder("bot_actions")
                .description("Действия с ссылками")
                .tag("action", "updateToken-3")
                .register(this.meterRegistry);
        updateTokenCounterSix = Counter.builder("bot_actions")
                .description("Действия с ссылками")
                .tag("action", "updateToken-6")
                .register(this.meterRegistry);
    }

    @Transactional(readOnly = true)
    public Mono<String> process(UUID userId, Long planId, Long orderId) {
        return Mono.fromCallable(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(UserNotFoundException::new);
                    if (tokenRepository.findByUser(user).isEmpty()) {
                        newTokenCounter.increment();
                        return newTokenService.process(userId, orderId).block();
                    } else {
                        switch (planId.intValue()) {
                            case 1:
                                updateTokenCounterOne.increment();
                                break;
                            case 2:
                                updateTokenCounterThree.increment();
                                break;
                            case 3:
                                updateTokenCounterSix.increment();
                                break;
                        }
                        return updateTokenService.process(userId, planId, orderId).block();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
