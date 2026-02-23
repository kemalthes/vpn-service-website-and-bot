package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.enums.TransactionType;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.enums.PaymentStatus;
import io.nesvpn.telegrambot.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TonPaymentService tonPaymentService;
    private final BalanceService balanceService;

    @Transactional
    public Payment createPayment(UUID userId, double amount, String method, String currency) {
        List<Payment> pendingPayments = paymentRepository.findByUserIdAndStatus(
                userId,
                PaymentStatus.PENDING.getValue()
        );

        if (pendingPayments.size() < 5) {

            BigDecimal roundedAmount = BigDecimal.valueOf(amount)
                    .setScale(2, RoundingMode.HALF_UP);

            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

            Payment payment = Payment.builder()
                .userId(userId)
                .amount(roundedAmount)
                .status(PaymentStatus.PENDING.getValue())
                .method(method)
                .currency(currency)
                .paymentDate(null)
                .expiresAt(expiresAt)
                .transactionToken(generateTransactionToken())
                .build();

            return paymentRepository.save(payment);
        }

        return null;

    }

    public Optional<Payment> getPaymentByToken(String token) {
        return paymentRepository.findByTransactionToken(token);
    }

    public List<Payment> getUserPendingPayments(UUID userId) {
        return paymentRepository.findByUserIdAndStatus(userId, PaymentStatus.PENDING.getValue());
    }

    private String generateTransactionToken() {
        return UUID.randomUUID().toString().substring(0, 18);
    }

    @Transactional
    public boolean checkAndConfirmCryptoPayment(String transactionToken) {
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionToken(transactionToken);

        if (paymentOpt.isEmpty()) {
            return false;
        }

        Payment payment = paymentOpt.get();

        if (LocalDateTime.now().isAfter(payment.getExpiresAt())) {
            payment.setStatus(PaymentStatus.EXPIRED.getValue());
            paymentRepository.save(payment);
            return false;
        }

        if (PaymentStatus.COMPLETED.getValue().equals(payment.getStatus())) {
            return true;
        }

        boolean isPaid = tonPaymentService.checkTransactionInBlockchain(payment);

        if (isPaid) {
            confirmPaymentAndUpdateBalance(payment);

            return true;
        }

        return false;
    }

    @Transactional
    public List<Payment> markExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(now);
        List<Payment> markedPayments = new ArrayList<>();

        for (Payment payment : expiredPayments) {
            if (PaymentStatus.PENDING.getValue().equals(payment.getStatus())) {
                payment.setStatus(PaymentStatus.EXPIRED.getValue());
                paymentRepository.save(payment);
                markedPayments.add(payment);
            }
        }

        return markedPayments;
    }

    @Transactional
    public boolean markPaymentAsExpired(String transactionId) {
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionToken(transactionId);

        if (paymentOpt.isEmpty()) {
            return false;
        }

        Payment payment = paymentOpt.get();

        if (!PaymentStatus.PENDING.getValue().equals(payment.getStatus())) {
            return false;
        }

        payment.setStatus(PaymentStatus.EXPIRED.getValue());
        paymentRepository.save(payment);

        return true;
    }

    @Transactional
    public boolean confirmPayment(String transactionToken) {
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionToken(transactionToken);

        if (paymentOpt.isEmpty()) {
            log.warn("Payment with token {} not found", transactionToken);
            return false;
        }

        Payment payment = paymentOpt.get();

        if (!PaymentStatus.PENDING.getValue().equals(payment.getStatus())) {
            log.warn("Payment {} is not PENDING, current status: {}",
                    payment.getId(), payment.getStatus());
            return false;
        }

        confirmPaymentAndUpdateBalance(payment);

        log.info("✅ Payment {} confirmed", payment.getId());
        return true;
    }

    private void confirmPaymentAndUpdateBalance (Payment payment) {
        payment.setStatus(PaymentStatus.COMPLETED.getValue());
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);
        balanceService.addBalance(payment.getUserId(), BigDecimal.valueOf(tonPaymentService.createUsdtPayment(payment).getAmountRub()),
                TransactionType.TOP_UP, "Пополнение баланса через USDT");
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING.getValue());
    }
}