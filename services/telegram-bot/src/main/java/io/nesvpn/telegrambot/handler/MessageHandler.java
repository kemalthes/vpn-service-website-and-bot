package io.nesvpn.telegrambot.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.dto.HwidDevice;
import io.nesvpn.telegrambot.dto.PlategaCreateResponse;
import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.enums.PaymentMethod;
import io.nesvpn.telegrambot.enums.PaymentStatus;
import io.nesvpn.telegrambot.model.*;
import io.nesvpn.telegrambot.services.*;
import io.nesvpn.telegrambot.services.ReferralService;
import io.nesvpn.telegrambot.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class MessageHandler {

    private final VpnBot vpnBot;
    private final UserService userService;
    private final ReferralService referralService;
    private final TelegramUserService telegramUserService;
    private final KeyboardFactory keyboardFactory;
    private final BalanceService balanceService;
    private final TokenService tokenService;
    private final VpnPlanService vpnPlanService;
    private final OrderService orderService;
    private final FloatRatesService floatRatesService;
    private final TonPaymentService tonPaymentService;
    private final PaymentService paymentService;
    private final CooldownService cooldownService;
    private final PlategaService plategaService;
    private final TextFactory textFactory;

    public MessageHandler(
            UserService userService,
            TelegramUserService telegramUserService,
            ReferralService referralService,
            KeyboardFactory keyboardFactory,
            BalanceService balanceService,
            TokenService tokenService,
            VpnPlanService vpnPlanService,
            @Lazy VpnBot vpnBot,
            OrderService orderService,
            FloatRatesService floatRatesService,
            TonPaymentService tonPaymentService,
            PaymentService paymentService,
            CooldownService cooldownService, PlategaService plategaService, TextFactory textFactory) {
        this.userService = userService;
        this.telegramUserService = telegramUserService;
        this.referralService = referralService;
        this.keyboardFactory = keyboardFactory;
        this.balanceService = balanceService;
        this.tokenService = tokenService;
        this.vpnPlanService = vpnPlanService;
        this.orderService = orderService;
        this.floatRatesService = floatRatesService;
        this.tonPaymentService = tonPaymentService;
        this.paymentService = paymentService;
        this.cooldownService = cooldownService;
        this.vpnBot = vpnBot;
        this.plategaService = plategaService;
        this.textFactory = textFactory;
    }

    public void handle(Message message) {
        String text = message.getText();
        if (text.startsWith("/start")) {
            handleStart(message);
        } else if (text.equals("/profile")) {
            handleProfile(message);
        } else if (text.equals("/referrals")) {
            handleReferrals(message);
        } else if (text.equals("/instructions")) {
            handleInstructions(message);
        } else if (text.equals("/balance")) {
            handleBalance(message);
        } else if (text.equals("/subscriptions")) {
            hanldeSubscription(message);
        } else if (text.equals("/info")) {
            handleAboutService(message);
        } else {
            TelegramUser telegramUser = telegramUserService.findOrCreate(message.getFrom().getId());

            if (telegramUser == null) {
                deleteMessage(message.getChatId(), message.getMessageId());
                handleStart(message);
                return;
            } 
            
            if (telegramUser.getState().equals(BotState.BALANCE_AWAITING_AMOUNT.toString())) {
                handleAmountInput(message);
            } else if (telegramUser.getState().equals(BotState.BALANCE_AWAITING_AMOUNT_CRYPTO.toString())) {
                handleAmountInputCrypto(message);
            } else {
                deleteMessage(message.getChatId(), message.getMessageId());
            }
        }
    }

    private void handleStart(Message message) {
        String text = message.getText();
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        boolean isNewUser = !userService.existsByTgId(userId);
        User user = userService.findOrCreateByTgId(userId);
        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.START);

        if (isNewUser && text.length() > 7) {
            String payload = text.substring(7);
            User referrer = userService.findByReferralCode(payload);
            if (referrer != null && !referrer.getTgId().equals(userId)) {
                referralService.linkReferral(user, referrer);

                String displayName = DisplayTelegramUsername.getDisplayName(vpnBot, user.getTgId());

                sendMessage(referrer.getTgId(), textFactory.newReferralText(displayName, user.getTgId()), null, "Markdown");
            }
        }
        if (isNewUser) {
            showSubscribeChannel(chatId, null);
        }
        showStart(chatId, null, user);
    }

    private void handleProfile(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.PROFILE);
        User user = userService.findOrCreateByTgId(userId);

        showProfile(chatId, null, user);
    }

    private void handleAboutService(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.INFO);

        showAboutService(chatId, null);
    }

    private void handleReferrals(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.REFERRALS);

        User user = userService.findOrCreateByTgId(userId);

        showReferrals(chatId, null, user);
    }

    private void handleBalance(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.BALANCE);

        User user = userService.findOrCreateByTgId(userId);

        showBalance(chatId, null, user);
    }

    private void handleInstructions(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.INSTRUCTIONS);

        User user = userService.findOrCreateByTgId(userId);
        showInstructions(chatId, null, user);
    }

    private void hanldeSubscription(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        telegramUserService.findOrCreate(userId);
        telegramUserService.setState(userId, BotState.SUBSCRIPTIONS);

        User user = userService.findOrCreateByTgId(userId);
        showSubscription(chatId, null, user);
    }

    public void handleAmountInput(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        User user = userService.findOrCreateByTgId(userId);

        try {
            int amount = Integer.parseInt(message.getText().trim().replaceAll("[^0-9]", ""));

            if (amount < 100) {
                sendError(chatId, """
                    ❌ Минимальная сумма пополнения — *100₽*
                    
                    Введите другую сумму:
                    """);

                showAwaitingBalance(chatId, null, user);
                return;
            }

            if (amount > 2000) {
                sendError(chatId, """
                    ❌ Максимальная сумма пополнения — *2000₽*
                    
                    Введите другую сумму:
                    """);

                showAwaitingBalance(chatId, null, user);
                return;
            }

            showPaymentSbp(chatId, amount, user);
        } catch (NumberFormatException e) {
            sendError(chatId, """
                ❌ Неверный формат суммы
                
                Введите число от *100* до *2000*:"
                """);

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
                sendError(chatId, """
                    ❌ Минимальная сумма пополнения — *1$*
                    
                    Введите другую сумму (1 - 25$):
                    """);

                showAwaitingBalanceWithCrypto(chatId, null, user);
                return;
            }

            if (amount > 25) {
                sendError(chatId, """
                    ❌ Максимальная сумма пополнения — *25$*
                    
                    Введите другую сумму:
                    """);

                showAwaitingBalanceWithCrypto(chatId, null, user);
                return;
            }

            showPaymentWithCrypto(chatId, amount, user);
        } catch (NumberFormatException e) {
            sendError(chatId, """
                ❌ Неверный формат суммы
                
                Введите число от *1$* до *25$*:"
                """);

            showAwaitingBalanceWithCrypto(chatId, null, user);
        }
    }

    public void showTopUp(Long chatId, Integer messageId) {
        telegramUserService.updateState(chatId, BotState.BALANCE_TOP_UP, BotState.BALANCE);

        editOrSendMessage(chatId, messageId, textFactory.topUpText(), keyboardFactory.getTopUpMenuInline(), "Markdown");
    }

    public void showAboutService(Long chatId, Integer messageId) {
        telegramUserService.updateState(chatId, BotState.INFO, BotState.START);

        editOrSendMessage(chatId, messageId, textFactory.aboutServiceText(), keyboardFactory.getInfoButton(), "HTML");
    }

    public void checkPayment(Long chatId, Integer messageId, String transactionId, User user) {
        Optional<Payment> paymentOpt = paymentService.getPaymentByToken(transactionId);

        if (paymentOpt.isEmpty()) {
            editMessageCaption(chatId, messageId, textFactory.checkPaymentErrorText(transactionId), null);
            return;
        }

        Payment payment = paymentOpt.get();
        PaymentStatus lastStatus = PaymentStatus.fromString(payment.getStatus());

        if (!cooldownService.canCheck(chatId)) {
            long remaining = cooldownService.getRemainingCooldown(chatId);
            String currentTime = Formatter.formatMoscow(LocalDateTime.now());

            editMessageCaption(chatId, messageId, textFactory.checkPaymentCooldownText(currentTime, remaining),
                    keyboardFactory.getPaymentCheckKeyboard(transactionId));
            return;
        }

        cooldownService.updateLastCheckTime(chatId);

        if (payment.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (!lastStatus.equals(PaymentStatus.EXPIRED)) {
                paymentService.markPaymentAsExpired(transactionId);
            }

            editMessageCaption(chatId, messageId, textFactory.expiredPaymentText(transactionId), null);
            return;
        }

        String baseText = textFactory.checkPaymentText(payment);

        boolean isPaid = paymentService.checkAndConfirmPayment(payment);

        if (!isPaid) {
            String currentTime = Formatter.formatMoscow(LocalDateTime.now());

            String fullText = baseText + textFactory.checkPaymentNotFoundText(currentTime);

            editMessageCaption(chatId, messageId, fullText,
                    keyboardFactory.getPaymentCheckKeyboard(transactionId));

        } else {
            editMessageCaption(chatId, messageId, baseText + "\n<b>Мы увидели ее, обрабатываем!</b>", null);
            if (lastStatus.equals(PaymentStatus.PENDING)) {
                User updatedUser = userService.getUserById(user.getId());
                showSuccessPayment(chatId, payment, updatedUser);
            }
        }
    }

    public void showExpiredPayment(Long chatId, String transactionId) {
        sendMessage(chatId, textFactory.expiredPaymentText(transactionId), keyboardFactory.getBackButton(), "HTML");
    }

    public void showSuccessPayment(Long chatId, Payment payment, User user) {
        String successText = textFactory.successText(payment, user);
        sendMessage(chatId, successText, keyboardFactory.getBackButton(), "HTML");
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


                String transactionId = plategaResponse.getTransactionId();
                String expiresIn = plategaResponse.getExpiresIn();
                String redirect = plategaResponse.getRedirect();

                LocalTime time = LocalTime.parse(expiresIn);

                LocalDateTime expiresAt = LocalDateTime.now()
                        .plusHours(time.getHour())
                        .plusMinutes(time.getMinute())
                        .plusSeconds(time.getSecond());

                Payment payment = paymentService.createPayment(user.getId(), amount, PaymentMethod.SBP.getValue(), currency, transactionId, expiresAt);

                String caption = textFactory.getPaymentTextSbp(payment);

                byte[] qrcode = Base64.getDecoder().decode(GenerateQrCode.generateQRCode(redirect));

                showPhotoDirectly(chatId, qrcode, caption, keyboardFactory.getPaymentCheckSbpKeyboard(payment));

                return;
            }
        } catch (Exception e) {
            log.error("Show payment with SBP", e);
        }

        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT_CRYPTO, BotState.BALANCE);
        showErrorCreatePayment(chatId, user);
    }

    public void showPaymentWithCrypto(Long chatId, double amount, User user) {
        try {
            Payment payment = paymentService.createPayment(user.getId(), amount, PaymentMethod.CRYPTO.getValue(), "USDT", UUID.randomUUID().toString(), LocalDateTime.now().plusMinutes(30));

            if (payment != null) {
                CryptoPayment cryptoPayment = tonPaymentService.createUsdtPayment(payment);
                byte[] qrBytes = Base64.getDecoder().decode(cryptoPayment.getQrCodeBase64());
                telegramUserService.updateState(chatId, BotState.BALANCE_AWAITING_AMOUNT, BotState.BALANCE_AWAITING_AMOUNT);

                String caption = textFactory.getPaymentTextCrypto(payment);

                showPhotoDirectly(chatId, qrBytes, caption, keyboardFactory.getPaymentCheckKeyboard(payment.getTransactionToken()));
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

        editOrSendMessage(chatId, null, textFactory.errorCreatePaymentText(pendingCount), keyboardFactory.getBackButton(), "HTML");
    }

    private void showPhotoDirectly(Long chatId, byte[] qrBytes, String caption, InlineKeyboardMarkup keyboardMarkup) {
        try {
            String botToken = vpnBot.getBotToken();
            String url = "https://api.telegram.org/bot" + botToken + "/sendPhoto";

            String boundary = "------------------------" + System.currentTimeMillis();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            writePart(outputStream, boundary, "chat_id", chatId.toString());

            writeFilePart(outputStream, boundary, "photo", "qr.png", qrBytes);

            writePart(outputStream, boundary, "caption", caption);

            writePart(outputStream, boundary, "parse_mode", "HTML");

            ObjectMapper mapper = new ObjectMapper();
            String replyMarkupJson = mapper.writeValueAsString(keyboardMarkup);
            writePart(outputStream, boundary, "reply_markup", replyMarkupJson);

            outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "multipart/form-data; charset=utf-8; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(outputStream.toByteArray()))
                    .build();

            client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        } catch (Exception e) {
            log.error("Send photo with Telegram API", e);
        }
    }

    private void writePart(ByteArrayOutputStream outputStream, String boundary, String name, String value) {
        try {
            outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write multipart data", e);
        }
    }

    private void writeFilePart(ByteArrayOutputStream outputStream, String boundary, String name, String filename, byte[] data) {
        try {
            outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(data);
            outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write multipart file data", e);
        }
    }

    public void showStart(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();

        telegramUserService.updateState(userId, BotState.START, BotState.START);

        String displayName = DisplayTelegramUsername.getDisplayName(vpnBot, user.getTgId());

        editOrSendMessage(chatId, messageId, textFactory.startText(displayName), keyboardFactory.getMainMenuInline(), "Markdown");
    }

    public void showProfile(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();

        telegramUserService.updateState(userId, BotState.PROFILE, BotState.START);

        String displayName = DisplayTelegramUsername.getDisplayName(vpnBot, user.getTgId());

        String referralLink = "https://t.me/" + vpnBot.getBotUsername() + "?start=" + user.getReferralCode();

        String createdAt = user.getCreatedAt() != null ? Formatter.formatMoscow(user.getCreatedAt()) : "Не указано";

        editOrSendMessage(chatId, messageId, textFactory.profileText(displayName, user.getBalance(), userService.getReferralsCount(user.getTgId()), createdAt, referralLink), keyboardFactory.getBackButton(), "Markdown");
    }

    public void showReferrals(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();
        telegramUserService.updateState(userId, BotState.REFERRALS, BotState.START);
        String referralLink = "https://t.me/" + vpnBot.getBotUsername() + "?start=" + user.getReferralCode();
        List<User> referrals = userService.getReferralsByReferrer(userId);
        record ReferralStat(User user, BigDecimal earnings) {}
        BigDecimal totalEarnings = BigDecimal.ZERO;
        List<ReferralStat> stats = new ArrayList<>();
        for (User referral : referrals) {
            BigDecimal earnings = referralService.getReferralEarnings(user.getId(), referral.getId());
            BigDecimal safeEarnings = (earnings != null) ? earnings : BigDecimal.ZERO;

            totalEarnings = totalEarnings.add(safeEarnings);
            stats.add(new ReferralStat(referral, safeEarnings));
        }
        stats.sort((a, b) -> b.earnings().compareTo(a.earnings()));
        StringBuilder referralsList = new StringBuilder();
        int referralsToShow = 15;
        int count = 0;
        for (ReferralStat stat : stats) {
            if (count >= referralsToShow) break;
            User referral = stat.user();
            String referralUsername = DisplayTelegramUsername.getDisplayName(vpnBot, referral.getTgId());
            String refInfo = String.format(
                    "%d) %s (id: %d) принес %.2f₽",
                    count + 1,
                    referralUsername != null ? referralUsername : "no_username",
                    referral.getTgId(),
                    stat.earnings()
            );
            referralsList.append(refInfo).append("\n");
            count++;
        }
        if (referrals.size() > referralsToShow) {
            referralsList.append(String.format("... и еще %d", referrals.size() - referralsToShow));
        }
        editOrSendMessage(chatId, messageId,
                textFactory.referralsText(referralLink, referrals.size(), totalEarnings, referralsList.toString()),
                keyboardFactory.getBackButton(), "HTML");
    }

    public void showInstructions(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();
        telegramUserService.updateState(userId, BotState.INSTRUCTIONS, BotState.SUBSCRIPTIONS);

        editOrSendMessage(chatId, messageId, textFactory.instructionsText(), keyboardFactory.getInstructionsMenu(), "Markdown");
    }

    public void showAndroidInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_ANDROID, BotState.INSTRUCTIONS);
        editMessage(chatId, messageId, textFactory.androidInstructionText(), keyboardFactory.getBackButton(), "Markdown");
    }

    public void showIosInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_IOS, BotState.INSTRUCTIONS);
        editMessage(chatId, messageId, textFactory.iosInstructionText(), keyboardFactory.getBackButton(), "Markdown");
    }

    public void showWindowsInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_WINDOWS, BotState.INSTRUCTIONS);
        editMessage(chatId, messageId, textFactory.windowsInstructionText(), keyboardFactory.getBackButton(), "Markdown");
    }

    public void showMacosInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_MACOS, BotState.INSTRUCTIONS);
        editMessage(chatId, messageId, textFactory.macosInstructionText(), keyboardFactory.getBackButton(), "Markdown");
    }

    public void showBalance(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE, BotState.START);
        editOrSendMessage(chatId, messageId, textFactory.balanceText(user.getBalance()), keyboardFactory.getBalanceMenu(), "Markdown");
    }

    public void showBalanceHistory(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_HISTORY, BotState.BALANCE);
        List<BalanceTransaction> history = balanceService.getHistory(user.getId());

        editOrSendMessage(chatId, messageId, textFactory.balanceHistoryText(history), keyboardFactory.getBackButton(), "HTML");
    }

    public void showAwaitingBalance(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT, BotState.BALANCE_TOP_UP);

        editOrSendMessage(chatId, messageId, textFactory.awaitingBalanceRubText(), keyboardFactory.getBackButton(), "Markdown");
    }

    public void showAwaitingBalanceWithCrypto(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT_CRYPTO, BotState.BALANCE_TOP_UP);

        editOrSendMessage(chatId, messageId, textFactory.awaitingBalanceCryptoText(floatRatesService.getUsdToRubRate()), keyboardFactory.getBackButton(), "Markdown");
    }

    public void showSubscribeChannel(Long chatId, Integer messageId) {
        editOrSendMessage(chatId, messageId, textFactory.channelSubscribeText(), keyboardFactory.getSubscribeChannelKeyboard(), "HTML");
    }

    public void showSubscription(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS, BotState.START);

        Token token = tokenService.getUserToken(user.getId());
        if (token == null) {
            editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
        } else {
            long daysLeft = tokenService.getDaysLeft(token);
            boolean isActive = token.isActive();
            String tokenUrl = tokenService.getFullTokenUrl(token);
            List<HwidDevice> hwidDevices = tokenService.getHwidDevicesByToken(token);
            Integer devicesCount = hwidDevices.size();
            String validTo = token.getValidTo() != null
                    ? Formatter.formatMoscow(token.getValidTo())
                    : "Не указано";

            editOrSendMessage(chatId, messageId, textFactory.subscriptionText(isActive, tokenUrl, validTo, daysLeft, devicesCount), keyboardFactory.getSubscriptionKeyboard(token.isActive(), devicesCount), "HTML");
        }
    }

    public void showHwidDevices(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTION_HWID_DEVICES, BotState.SUBSCRIPTIONS);

        Token token = tokenService.getUserToken(user.getId());

        if (token == null) {
            editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        List<HwidDevice> hwidDevices = tokenService.getHwidDevicesByToken(token);

        editOrSendMessage(chatId, messageId, textFactory.hwidDevicesText(hwidDevices), keyboardFactory.getHwidDevicesKeyboard(hwidDevices), "HTML");
    }

    public void showDeleteHwidDeviceConfirm(Long chatId, Integer messageId, String hwid, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.SUBSCRIPTION_HWID_DEVICES_CONFIRM, BotState.SUBSCRIPTION_HWID_DEVICES);
        editOrSendMessage(chatId, messageId, textFactory.hwidDeviceDeleteConfirm(), keyboardFactory.getHwidDeviceDeleteKeyboard(hwid), "HTML");
    }

    public void showDeleteHwidDevice(Long chatId, Integer messageId, String hwid, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS, BotState.SUBSCRIPTION_HWID_DEVICES);
        Token token = tokenService.getUserToken(user.getId());

        if (token == null) {
            editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        boolean isSuccess = tokenService.deleteHwidDeviceByToken(token, hwid);
        editOrSendMessage(chatId, messageId, isSuccess ? textFactory.hwidDeviceDeleteSuccess() : textFactory.hwidDeviceDeleteError(), keyboardFactory.getBackButton(), "HTML");
    }

    public void showSubscriptionExtend(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS_EXTEND, BotState.SUBSCRIPTIONS);

        Token token = tokenService.getUserToken(user.getId());

        if (token == null) {
            editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        long daysLeft = tokenService.getDaysLeft(token);

        String validTo = token.getValidTo() != null
                ? Formatter.formatMoscow(token.getValidTo())
                : "Не указано";

        List<VpnPlan> plans = vpnPlanService.getAllPlans();

        editOrSendMessage(chatId, messageId, textFactory.extendSubscriptionText(user.getBalance(), validTo, daysLeft), keyboardFactory.getExtendPlansKeyboard(token.getId(), plans), "HTML");
    }

    public void showSubscriptionExpiring(Long chatId, String validTo) {
        editOrSendMessage(chatId, null, textFactory.subscribeExpiringText(validTo), keyboardFactory.getExpiringSubscriptionMenu(), "HTML");
    }

    public void showExtendConfirm(Long chatId, Integer messageId, Long tokenId, Long planId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS_CONFIRM, BotState.SUBSCRIPTIONS_EXTEND);

        Token token = tokenService.findById(tokenId).orElse(null);
        VpnPlan plan = vpnPlanService.findById(planId).orElse(null);

        if (token == null) {
            editOrSendMessage(chatId, messageId, textFactory.tokenNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        } else if (plan == null) {
            editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        LocalDateTime currentValidTo = token.getValidTo() != null ? token.getValidTo() : LocalDateTime.now();

        LocalDateTime baseDate = currentValidTo.isBefore(LocalDateTime.now())
                ? LocalDateTime.now()
                : currentValidTo;

        LocalDateTime newValidTo = baseDate.plusDays(plan.getDuration());

        editOrSendMessage(chatId, messageId, textFactory.extendSubscribeConfirmText(plan.getName(), plan.getPrice(), Formatter.formatMoscow(currentValidTo), Formatter.formatMoscow(newValidTo), user.getBalance()), keyboardFactory.getConfirmExtendKeyboard(tokenId, planId), "HTML");
    }

    public void showExtendProcess(Long chatId, Integer messageId, Long tokenId, Long planId, User user) {
        Optional<Token> token = tokenService.findById(tokenId);
        VpnPlan plan = vpnPlanService.findById(planId).orElse(null);

        if (token.isEmpty() || plan == null) {
            editOrSendMessage(chatId, messageId, textFactory.dataNotFoundText(), keyboardFactory.getBackButton(), "HTML");
            return;
        }

        if (user.getBalance().compareTo(BigDecimal.valueOf(plan.getPrice())) < 0) {
            editOrSendMessage(chatId, messageId, textFactory.notEnoughMoneyMessage(plan.getPrice(), user.getBalance()), keyboardFactory.getBackToSubscriptionKeyboard(), "HTML");
            return;
        }



        Order order = orderService.createOrder(user, plan);
        log.info("Заказ {} cоздан", order.getId());

        editOrSendMessage(chatId, messageId, textFactory.successSubscribeProvidedText(), null, "HTML");

        showStart(chatId, null, user);
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup markup, String parseMode) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        if (markup != null) {
            message.setReplyMarkup(markup);
        }

        message.setParseMode(parseMode);

        try {
            vpnBot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }

    private void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);
        try {
            vpnBot.execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }

    private void editMessageCaption(Long chatId, Integer messageId, String caption, InlineKeyboardMarkup keyboard) {
        try {
            EditMessageCaption editCaption = new EditMessageCaption();
            editCaption.setChatId(chatId.toString());
            editCaption.setMessageId(messageId);
            editCaption.setCaption(caption);
            editCaption.setParseMode("HTML");

            if (keyboard != null) {
                editCaption.setReplyMarkup(keyboard);
            }

            vpnBot.execute(editCaption);
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }

    private void sendError(Long chatId, String errorText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(errorText);
        message.setParseMode("Markdown");

        try {
            vpnBot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }

    private void editMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup markup, String parseMode) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode(parseMode);
            editMessage.disableWebPagePreview();
            if (markup != null) {
                editMessage.setReplyMarkup(markup);
            }
            vpnBot.execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }

    private void editOrSendMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup markup, String parseMode) {
        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text.trim());
                if (markup != null) {
                    editMessage.setReplyMarkup(markup);
                }
                editMessage.setParseMode(parseMode);
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                if (markup != null) {
                    sendMessage.setReplyMarkup(markup);
                }
                sendMessage.setParseMode(parseMode);
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API Exception", e);
        }
    }
}
