package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.Lucky777Spin;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Lucky777SpinRepository extends JpaRepository<Lucky777Spin, UUID> {

    Optional<Lucky777Spin> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select spin from Lucky777Spin spin where spin.userId = :userId")
    Optional<Lucky777Spin> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select spin
            from Lucky777Spin spin
            where spin.lastSpinAt is not null
              and spin.lastSpinAt <= :availableBefore
              and (
                    spin.lastAvailableNotificationAt is null
                    or spin.lastAvailableNotificationAt < spin.lastSpinAt
                  )
            order by spin.lastSpinAt asc
            """)
    List<Lucky777Spin> findAvailableForNotificationForUpdate(
            @Param("availableBefore") LocalDateTime availableBefore,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select spin from Lucky777Spin spin where spin.userId in :userIds")
    List<Lucky777Spin> findAllByUserIdInForUpdate(@Param("userIds") Collection<UUID> userIds);

    @Modifying
    @Query(value = "INSERT INTO lucky_777_spins (user_id) VALUES (:userId) ON CONFLICT (user_id) DO NOTHING", nativeQuery = true)
    void insertIfMissing(@Param("userId") UUID userId);
}
