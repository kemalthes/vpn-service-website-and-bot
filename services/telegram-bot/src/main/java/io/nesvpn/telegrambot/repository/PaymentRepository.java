package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionToken(String transactionToken);

    List<Payment> findByUserIdAndStatus(UUID userId, String status);

    List<Payment> findByStatus(String status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.expiresAt < :currentTime")
    List<Payment> findExpiredPayments(@Param("currentTime") LocalDateTime currentTime);

}