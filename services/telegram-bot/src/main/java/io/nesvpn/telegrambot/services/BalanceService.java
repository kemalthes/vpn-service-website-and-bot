package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.model.BalanceTransaction;
import io.nesvpn.telegrambot.model.TransactionType;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.repository.BalanceTransactionRepository;
import io.nesvpn.telegrambot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserRepository userRepository;
    private final BalanceTransactionRepository transactionRepository;

    @Transactional
    public void addBalance(UUID userId, BigDecimal amount, TransactionType type, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        BalanceTransaction transaction = new BalanceTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transactionRepository.save(transaction);

        if (type == TransactionType.SUBSCRIPTION_PURCHASE && amount.compareTo(BigDecimal.ZERO) < 0) {
            processReferralBonus(user, amount.abs());
        }
    }

    private void processReferralBonus(User buyer, BigDecimal purchaseAmount) {
        if (buyer.getReferredBy() != null) {
            User referrer = userRepository.findByTgId(buyer.getReferredBy())
                    .orElse(null);

            if (referrer != null) {
                BigDecimal bonus = purchaseAmount.multiply(new BigDecimal("0.20"));

                referrer.setBalance(referrer.getBalance().add(bonus));
                userRepository.save(referrer);

                BalanceTransaction referralTx = new BalanceTransaction();
                referralTx.setUserId(referrer.getId());
                referralTx.setAmount(bonus);
                referralTx.setType(TransactionType.REFERRAL_BONUS);
                referralTx.setDescription(
                        String.format("20%% с покупки друга id: %s (%.2f₽)",
                                buyer.getTgId(), purchaseAmount)
                );
                referralTx.setReferralId(buyer.getId());

                transactionRepository.save(referralTx);
            }
        }
    }

    @Transactional
    public void subtractBalance(UUID userId, BigDecimal amount, TransactionType type, String description) {
        addBalance(userId, amount.negate(), type, description);
    }

    @Transactional(readOnly = true)
    public List<BalanceTransaction> getHistory(UUID userId) {
        return transactionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getReferralEarnings(UUID referrerId, UUID referralId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(referrerId)
                .stream()
                .filter(tx -> tx.getType() == TransactionType.REFERRAL_BONUS)
                .filter(tx -> referralId.equals(tx.getReferralId()))
                .map(BalanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
