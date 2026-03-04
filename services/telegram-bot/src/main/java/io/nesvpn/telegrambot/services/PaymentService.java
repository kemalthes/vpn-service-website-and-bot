package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.enums.PaymentMethod;
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
    private final PlategaService plategaService;

    @Transactional
    public Payment createPayment(UUID userId, double amount, String method, String currency, String transactionToken, LocalDateTime expiresAt) {
        List<Payment> pendingPayments = paymentRepository.findByUserIdAndStatus(
                userId,
                PaymentStatus.PENDING.getValue()
        );

        if (pendingPayments.size() < 5) {
            BigDecimal roundedAmount = BigDecimal.valueOf(amount)
                    .setScale(2, RoundingMode.HALF_UP);

            Payment payment = Payment.builder()
                .userId(userId)
                .amount(roundedAmount)
                .status(PaymentStatus.PENDING.getValue())
                .method(method)
                .currency(currency)
                .paymentDate(null)
                .expiresAt(expiresAt)
                .transactionToken(transactionToken)
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

    @Transactional
    public boolean checkAndConfirmPayment(Payment payment) {
        if (LocalDateTime.now().isAfter(payment.getExpiresAt())) {
            payment.setStatus(PaymentStatus.EXPIRED.getValue());
            paymentRepository.save(payment);
            return false;
        }

        if (PaymentStatus.COMPLETED.getValue().equals(payment.getStatus())) {
            return true;
        }


        boolean isPaid = false;

        if (payment.getMethod().equals(PaymentMethod.CRYPTO.getValue())) {
            isPaid = tonPaymentService.checkTransactionInBlockchain(payment);
        } else {
            isPaid = plategaService.checkPayment(payment);
        }

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
        BigDecimal amount = BigDecimal.ZERO;
        String description = "Пополнение";

        if (payment.getMethod().equals(PaymentMethod.CRYPTO.getValue())) {
            amount = BigDecimal.valueOf(tonPaymentService.createUsdtPayment(payment).getAmountRub());
            description = "Пополнение баланса через USDT";
        } else if  (payment.getMethod().equals(PaymentMethod.SBP.getValue())) {
            amount = payment.getAmount();
            description = "Пополнение баланса через СБП";
        }
        balanceService.addBalance(payment.getUserId(), amount,
                TransactionType.TOP_UP, description);
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING.getValue());
    }
}