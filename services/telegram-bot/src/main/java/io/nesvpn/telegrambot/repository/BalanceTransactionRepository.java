package io.nesvpn.telegrambot.repository;

import io.nesvpn.telegrambot.model.BalanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BalanceTransactionRepository extends JpaRepository<BalanceTransaction, Long> {

    List<BalanceTransaction> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);

    List<BalanceTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
}