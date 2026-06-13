package io.nesvpn.telegrambot.handler.sections;

import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.dto.PlategaCreateResponse;
import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.enums.PaymentMethod;
import io.nesvpn.telegrambot.enums.PaymentStatus;
import io.nesvpn.telegrambot.handler.common.TelegramMessageSender;
import io.nesvpn.telegrambot.model.BalanceTransaction;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.BalanceService;
import io.nesvpn.telegrambot.services.CooldownService;
import io.nesvpn.telegrambot.services.FloatRatesService;
import io.nesvpn.telegrambot.services.PaymentService;
import io.nesvpn.telegrambot.services.PlategaService;
import io.nesvpn.telegrambot.services.TelegramUserService;
import io.nesvpn.telegrambot.services.TonPaymentService;
import io.nesvpn.telegrambot.services.UserService;
import io.nesvpn.telegrambot.util.Formatter;
import io.nesvpn.telegrambot.util.GenerateQrCode;
import io.nesvpn.telegrambot.util.KeyboardFactory;
import io.nesvpn.telegrambot.util.TextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalancePaymentHandler {

    private final UserService userService;
    private final TelegramUserService telegramUserService;
    private final BalanceService balanceService;
    private final PaymentService paymentService;
    private final CooldownService cooldownService;
    private final PlategaService plategaService;
    private final FloatRatesService floatRatesService;
    private final TonPaymentService tonPaymentService;
    private final TextFactory textFactory;
    private final KeyboardFactory keyboardFactory;
    private final TelegramMessageSender sender;

    public void handleBalance(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.BALANCE);

        User user = userService.findOrCreateByTgId(userId);
        showBalance(chatId, null, user);
    }

    public void handleAmountInput(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        User user = userService.findOrCreateByTgId(userId);

        try {
            int amount = Integer.parseInt(message.getText().trim().replaceAll("[^0-9]", ""));

            if (amount < 100) {
                sender.sendError(
                        chatId,
                        textFactory.inputErrorText("Минимальная сумма пополнения — 100₽", "Введите другую сумму:")
                );
                showAwaitingBalance(chatId, null, user);
                return;
            }

            if (amount > 2000) {
                sender.sendError(
                        chatId,
                        textFactory.inputErrorText("Максимальная сумма пополнения — 2000₽", "Введите другую сумму:")
                );
                showAwaitingBalance(chatId, null, user);
                return;
            }

            showPaymentSbp(chatId, amount, user);
        } catch (NumberFormatException e) {
            sender.sendError(
                    chatId,
                    textFactory.inputErrorText(
                            "Неверный формат суммы",
                            "Введите число от <b>100</b> до <b>2000</b>:"
                    )
            );
            showAwaitingBalance(chatId, null, user);
        }
    }

    public void handleAmountInputCrypto(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        User user = userService.findOrCreateByTgId(userId);

        try {
            double amount = Double.parseDouble(message.getText().trim().replaceAll("[^0-9,.]", ""));
            if (amount < 1) {
                sender.sendError(
                        chatId,
                        textFactory.inputErrorText("Минимальная сумма пополнения — 1$", "Введите другую сумму (1 - 25$):")
                );
                showAwaitingBalanceWithCrypto(chatId, null, user);
                return;
            }

            if (amount > 25) {
                sender.sendError(
                        chatId,
                        textFactory.inputErrorText("Максимальная сумма пополнения — 25$", "Введите другую сумму:")
                );
                showAwaitingBalanceWithCrypto(chatId, null, user);
                return;
            }

            showPaymentWithCrypto(chatId, amount, user);
        } catch (NumberFormatException e) {
            sender.sendError(
                    chatId,
                    textFactory.inputErrorText(
                            "Неверный формат суммы",
                            "Введите число от <b>1$</b> до <b>25$</b>:"
                    )
            );
            showAwaitingBalanceWithCrypto(chatId, null, user);
        }
    }

    public void showTopUp(Long chatId, Integer messageId) {
        telegramUserService.updateState(chatId, BotState.BALANCE_TOP_UP, BotState.BALANCE);
        sender.editOrSendMessage(chatId, messageId, textFactory.topUpText(), keyboardFactory.getTopUpMenuInline(), "HTML");
    }

    public void showBalance(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE, BotState.START);
        sender.editOrSendMessage(chatId, messageId, textFactory.balanceText(user.getBalance()), keyboardFactory.getBalanceMenu(), "HTML");
    }

    public void showBalanceHistory(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_HISTORY, BotState.BALANCE);
        List<BalanceTransaction> history = balanceService.getHistory(user.getId());

        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.balanceHistoryText(history),
                keyboardFactory.getBackButton(),
                "HTML"
        );
    }

    public void showAwaitingBalance(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT, BotState.BALANCE_TOP_UP);
        sender.editOrSendMessage(chatId, messageId, textFactory.awaitingBalanceRubText(), keyboardFactory.getBackButton(), "HTML");
    }

    public void showAwaitingBalanceWithCrypto(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT_CRYPTO, BotState.BALANCE_TOP_UP);
        sender.editOrSendMessage(
                chatId,
                messageId,
                textFactory.awaitingBalanceCryptoText(floatRatesService.getUsdToRubRate()),
                keyboardFactory.getBackButton(),
                "HTML"
        );
    }

    public void checkPayment(Long chatId, Integer messageId, String transactionId, User user) {
        Optional<Payment> paymentOpt = paymentService.getPaymentByToken(transactionId);

        if (paymentOpt.isEmpty()) {
            sender.editMessageCaption(chatId, messageId, textFactory.checkPaymentErrorText(transactionId), null);
            return;
        }

        Payment payment = paymentOpt.get();
        PaymentStatus lastStatus = PaymentStatus.fromString(payment.getStatus());

        if (!cooldownService.canCheck(chatId)) {
            long remaining = cooldownService.getRemainingCooldown(chatId);
            String currentTime = Formatter.formatMoscow(LocalDateTime.now());

            sender.editMessageCaption(
                    chatId,
                    messageId,
                    textFactory.checkPaymentCooldownText(currentTime, remaining),
                    keyboardFactory.getPaymentCheckKeyboard(transactionId)
            );
            return;
        }

        cooldownService.updateLastCheckTime(chatId);

        if (payment.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (!lastStatus.equals(PaymentStatus.EXPIRED)) {
                paymentService.markPaymentAsExpired(transactionId);
            }

            sender.editMessageCaption(chatId, messageId, textFactory.expiredPaymentText(transactionId), null);
            return;
        }

        String baseText = textFactory.checkPaymentText(payment);
        boolean isPaid = paymentService.checkAndConfirmPayment(payment);

        if (!isPaid) {
            String currentTime = Formatter.formatMoscow(LocalDateTime.now());
            String fullText = baseText + textFactory.checkPaymentNotFoundText(currentTime);

            sender.editMessageCaption(
                    chatId,
                    messageId,
                    fullText,
                    keyboardFactory.getPaymentCheckKeyboard(transactionId)
            );
        } else {
            sender.editMessageCaption(chatId, messageId, baseText + "\n<b>Мы увидели ее, обрабатываем!</b>", null);
            if (lastStatus.equals(PaymentStatus.PENDING)) {
                User updatedUser = userService.getUserById(user.getId());
                showSuccessPayment(chatId, payment, updatedUser);
            }
        }
    }

    public void showExpiredPayment(Long chatId, String transactionId) {
        sender.sendMessage(chatId, textFactory.expiredPaymentText(transactionId), keyboardFactory.getBackButton(), "HTML");
    }

    public void showSuccessPayment(Long chatId, Payment payment, User user) {
        String successText = textFactory.successText(payment, user);
        sender.sendMessage(chatId, successText, keyboardFactory.getBackButton(), "HTML");
    }

    public void showPaymentSbp(Long chatId, int amount, User user) {
        telegramUserService.updateState(chatId, BotState.PAYMENT_AWAITING_CONFIRMATION, BotState.BALANCE_AWAITING_AMOUNT);

        try {
            if (paymentService.getUserPendingPayments(user.getId()).size() < 5) {
                String currency = "RUB";
                PlategaCreateResponse plategaResponse = plategaService.createTransaction(
                        amount,
                        currency,
                        "Пополнение баланса NesVPN",
                        "Пополнение из бота"
                );

                log.info(
                        String.format(
                                "transaction: %s, expiresIn: %s, ",
                                plategaResponse.getTransactionId(),
                                plategaResponse.getExpiresIn()
                        )
                );

                String transactionId = plategaResponse.getTransactionId();
                String expiresIn = plategaResponse.getExpiresIn() != null ? plategaResponse.getExpiresIn() : "00:30:00";
                String redirect = plategaResponse.getRedirect();

                LocalTime time = LocalTime.parse(expiresIn);
                LocalDateTime expiresAt = LocalDateTime.now()
                        .plusHours(time.getHour())
                        .plusMinutes(time.getMinute())
                        .plusSeconds(time.getSecond());

                Payment payment = paymentService.createPayment(
                        user.getId(),
                        amount,
                        PaymentMethod.SBP.getValue(),
                        currency,
                        transactionId,
                        expiresAt
                );

                String caption = textFactory.getPaymentTextSbp(payment);
                byte[] qrcode = Base64.getDecoder().decode(GenerateQrCode.generateQRCode(redirect));

                sender.showPhotoDirectly(chatId, qrcode, caption, keyboardFactory.getPaymentCheckSbpKeyboard(payment));
                return;
            }
        } catch (Exception e) {
            log.error("Show payment with SBP", e);
            sender.sendMessage(chatId, textFactory.errorPlategaText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT_CRYPTO, BotState.BALANCE);
        showErrorCreatePayment(chatId, user);
    }

    public void showPaymentWithCrypto(Long chatId, double amount, User user) {
        try {
            Payment payment = paymentService.createPayment(
                    user.getId(),
                    amount,
                    PaymentMethod.CRYPTO.getValue(),
                    "USDT",
                    UUID.randomUUID().toString(),
                    LocalDateTime.now().plusMinutes(30)
            );

            if (payment != null) {
                CryptoPayment cryptoPayment = tonPaymentService.createUsdtPayment(payment);
                byte[] qrBytes = Base64.getDecoder().decode(cryptoPayment.getQrCodeBase64());
                telegramUserService.updateState(chatId, BotState.BALANCE_AWAITING_AMOUNT, BotState.BALANCE_AWAITING_AMOUNT);

                String caption = textFactory.getPaymentTextCrypto(payment);
                sender.showPhotoDirectly(
                        chatId,
                        qrBytes,
                        caption,
                        keyboardFactory.getPaymentCheckKeyboard(payment.getTransactionToken())
                );
            } else {
                telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT_CRYPTO, BotState.BALANCE);
                showErrorCreatePayment(chatId, user);
            }
        } catch (Exception e) {
            log.error("Show payment with CRYPTO", e);
        }
    }

    private void showErrorCreatePayment(Long chatId, User user) {
        List<Payment> pendingPayments = paymentService.getUserPendingPayments(user.getId());
        int pendingCount = pendingPayments.size();

        sender.editOrSendMessage(
                chatId,
                null,
                textFactory.errorCreatePaymentText(pendingCount),
                keyboardFactory.getBackButton(),
                "HTML"
        );
    }
}
