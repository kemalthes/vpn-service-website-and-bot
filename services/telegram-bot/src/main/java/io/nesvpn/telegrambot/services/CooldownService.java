package io.nesvpn.telegrambot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CooldownService {

    private final Map<Long, Long> lastCheckTime = new ConcurrentHashMap<>();

    private static final long COOLDOWN_MS = 10000;

    public boolean canCheck(Long chatId) {
        Long lastTime = lastCheckTime.get(chatId);
        if (lastTime == null) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastTime;

        return timePassed >= COOLDOWN_MS;
    }

    public long getRemainingCooldown(Long chatId) {
        Long lastTime = lastCheckTime.get(chatId);
        if (lastTime == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastTime;

        if (elapsed >= COOLDOWN_MS) {
            return 0;
        }

        return (COOLDOWN_MS - elapsed) / 1000;
    }

    public void updateLastCheckTime(Long chatId) {
        lastCheckTime.put(chatId, System.currentTimeMillis());
        log.debug("Updated last check time for user {}", chatId);
    }
}