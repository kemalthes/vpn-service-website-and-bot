package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.rabbit.LinkRequest;
import io.nesvpn.telegrambot.rabbit.LinkRequestProducer;
import io.nesvpn.telegrambot.repository.UserRepository;
import io.nesvpn.telegrambot.services.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.generics.TelegramBot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ReferralService referralService;
    private final LinkRequestProducer linkRequestProducer;

    public User findOrCreateByTgId(Long tgId) {
        return userRepository.findByTgId(tgId)
                .orElseGet(() -> createTelegramUser(tgId));
    }

    private User createTelegramUser(Long tgId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTgId(tgId);
        user.setRole("user");
        user.setReferralCode(referralService.generateReferralCode(tgId));
        user.setBalance(BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now());

        log.info("User created: {}", user);
        // TODO: создать для него ссылку на 3 дня, с максимум устройствами 3, и всего гб 30 гб
        linkRequestProducer.sendLinkGenerationTask(LinkRequest.builder().userId(user.getId()).build());

        return userRepository.save(user);
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User findByReferralCode(String referralCode) {
        return userRepository.findByReferralCode(referralCode).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<User> getReferralsByReferrer(Long referrerId) {
        return userRepository.findAllByReferredBy(referrerId);
    }

    public List<User> getReferrals(Long referrerTgId) {
        return userRepository.findAllByReferredBy(referrerTgId);
    }

    public boolean existsByTgId(Long tgId) {
        return userRepository.existsByTgId(tgId);
    }

    public int getReferralsCount(Long tgId) {
        return userRepository.countByReferredBy(tgId);
    }
}
