package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.enums.SubscriptionExpirationNotificationType;
import io.nesvpn.telegrambot.model.Token;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query("SELECT t FROM Token t WHERE t.userId = :userId ORDER BY t.createdAt DESC LIMIT 1")
    Optional<Token> findByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
            FROM Token t
            WHERE LOWER(t.status) = 'active'
              AND t.validTo IS NOT NULL
              AND t.validTo > :windowStart
              AND t.validTo <= :windowEnd
              AND NOT EXISTS (
                    SELECT sameDayNotification.id
                    FROM SubscriptionExpirationNotification sameDayNotification
                    WHERE sameDayNotification.tokenId = t.id
                      AND sameDayNotification.sentAt >= :currentDayStart
              )
              AND NOT EXISTS (
                    SELECT n.id
                    FROM SubscriptionExpirationNotification n
                    WHERE n.tokenId = t.id
                      AND n.tokenValidTo = t.validTo
                      AND n.notificationType IN :alreadySentTypes
              )
            ORDER BY t.validTo ASC
            """)
    List<Token> findDueActiveSubscriptionExpirationTokens(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd,
            @Param("currentDayStart") LocalDateTime currentDayStart,
            @Param("alreadySentTypes") Collection<SubscriptionExpirationNotificationType> alreadySentTypes,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t
            FROM Token t
            WHERE t.validTo IS NOT NULL
              AND t.validTo > :windowStart
              AND t.validTo <= :windowEnd
              AND NOT EXISTS (
                    SELECT sameDayNotification.id
                    FROM SubscriptionExpirationNotification sameDayNotification
                    WHERE sameDayNotification.tokenId = t.id
                      AND sameDayNotification.sentAt >= :currentDayStart
              )
              AND NOT EXISTS (
                    SELECT n.id
                    FROM SubscriptionExpirationNotification n
                    WHERE n.tokenId = t.id
                      AND n.tokenValidTo = t.validTo
                      AND n.notificationType IN :alreadySentTypes
              )
            ORDER BY t.validTo ASC
            """)
    List<Token> findDueExpiredSubscriptionExpirationTokens(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd,
            @Param("currentDayStart") LocalDateTime currentDayStart,
            @Param("alreadySentTypes") Collection<SubscriptionExpirationNotificationType> alreadySentTypes,
            Pageable pageable);
}
