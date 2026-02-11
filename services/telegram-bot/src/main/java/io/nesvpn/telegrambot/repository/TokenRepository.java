package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query("SELECT t FROM Token t WHERE t.userId = :userId ORDER BY t.createdAt DESC LIMIT 1")
    Optional<Token> findByUserId(@Param("userId") UUID userId);

}
