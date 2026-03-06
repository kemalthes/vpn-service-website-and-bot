package io.nesvpn.telegrambot.services;

import io.nesvpn.telegrambot.handler.MessageHandler;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        if (pendingPayments.isEmpty()) {
            return;
        }
        List<Payment> confirmedPayments = new ArrayList<>();
        for (Payment payment : pendingPayments) {
            try {
                boolean confirmed = paymentService.checkAndConfirmPayment(payment);
                if (confirmed) {
                    confirmedPayments.add(payment);
                }
            } catch (Exception e) {
                log.error("Error checking payment {}", payment.getId(), e);
            }
        }
        if (confirmedPayments.isEmpty()) {
            return;
        }
        List<UUID> userIds = confirmedPayments.stream()
                .map(Payment::getUserId)
                .distinct()
                .toList();
        List<User> users = userService.getUsersByIds(userIds);
        Map<UUID, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (Payment payment : confirmedPayments) {
            User user = userMap.get(payment.getUserId());
            if (user != null) {
                messageHandler.showSuccessPayment(user.getTgId(), payment, user);
            }
        }
    }
}