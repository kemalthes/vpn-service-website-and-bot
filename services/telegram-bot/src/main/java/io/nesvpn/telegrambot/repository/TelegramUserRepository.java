package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByTgId(Long tgId);

    boolean existsByTgId(Long tgId);
}
