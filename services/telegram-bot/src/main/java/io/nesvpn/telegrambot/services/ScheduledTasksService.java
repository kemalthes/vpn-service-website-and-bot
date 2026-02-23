package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.handler.MessageHandler;
import io.nesvpn.telegrambot.handler.VpnBot;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasksService {

    private final PaymentService paymentService;
    private final TonPaymentService tonPaymentService;
    private final MessageHandler messageHandler;
    private final UserService userService;

    @Scheduled(fixedRate = 300000)
    public void checkExpiredPayments() {
        List<Payment> markedPayment = paymentService.markExpiredPayments();
        for (Payment payment : markedPayment) {
            User user = userService.getUserById(payment.getUserId());
            messageHandler.showExpiredPayment(user.getTgId(), payment.getTransactionToken());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkPendingPayments() {
        List<Payment> pendingPayments = paymentService.getPendingPayments();

        for (Payment payment : pendingPayments) {
            try {
                boolean confirmed = tonPaymentService.checkTransactionInBlockchain(payment);
                if (confirmed) {
                    paymentService.confirmPayment(payment.getTransactionToken());
                    User user = userService.getUserById(payment.getUserId());
                    if (user != null) {
                        messageHandler.showSuccessPayment(user.getTgId(), tonPaymentService.createUsdtPayment(payment), user);
                    }
                }
            } catch (Exception e) {
                log.error("Error checking payment {}", payment.getId(), e);
            }
        }
    }
}