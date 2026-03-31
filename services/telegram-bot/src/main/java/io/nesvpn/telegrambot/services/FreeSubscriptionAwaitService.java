package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.model.Token;
import io.nesvpn.telegrambot.model.User;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreeSubscriptionAwaitService {
    private static final int MAX_ATTEMPTS = 8;
    private static final long INITIAL_DELAY_SECONDS = 1;
    private static final long POLL_DELAY_SECONDS = 1;

    private final TokenService tokenService;
    private final UserService userService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void waitForFreeSubscriptionAndShow(Long chatId, User user, Consumer<User> onTokenReady, Runnable onTimeout) {
        AtomicInteger attempt = new AtomicInteger(1);
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                Token token = tokenService.getUserToken(user.getId());

                if (token != null) {
                    User refreshedUser = userService.getUserById(user.getId());
                    onTokenReady.accept(refreshedUser != null ? refreshedUser : user);
                    cancelFuture(futureRef);
                    return;
                }
                if (attempt.incrementAndGet() > MAX_ATTEMPTS) {
                    onTimeout.run();
                    cancelFuture(futureRef);
                }
            } catch (Exception e) {
                log.error("Error while checking free subscription for chatId: {}", chatId, e);
            }
        }, INITIAL_DELAY_SECONDS, POLL_DELAY_SECONDS, TimeUnit.SECONDS);
        futureRef.set(future);
    }

    @PreDestroy
    void shutdownScheduler() {
        scheduler.shutdownNow();
    }

    private void cancelFuture(AtomicReference<ScheduledFuture<?>> futureRef) {
        ScheduledFuture<?> future = futureRef.get();
        if (future != null) {
            future.cancel(false);
        }
    }
}
