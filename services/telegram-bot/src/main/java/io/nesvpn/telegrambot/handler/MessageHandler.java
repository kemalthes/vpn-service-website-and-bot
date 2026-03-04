package io.nesvpn.telegrambot.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.dto.PlategaCreateResponse;
import io.nesvpn.telegrambot.enums.BotState;
import io.nesvpn.telegrambot.enums.PaymentMethod;
import io.nesvpn.telegrambot.enums.PaymentStatus;
import io.nesvpn.telegrambot.enums.TransactionType;
import io.nesvpn.telegrambot.model.*;
import io.nesvpn.telegrambot.rabbit.LinkRequestProducer;
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
import java.time.format.DateTimeFormatter;
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
    private final LinkRequestProducer linkRequestProducer;
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
            @Lazy VpnBot vpnBot, LinkRequestProducer linkRequestProducer,
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
        this.linkRequestProducer = linkRequestProducer;
        this.plategaService = plategaService;
        this.textFactory = textFactory;
    }

    public void handle(Message message) {
        String text = message.getText();

        if (!telegramUserService.existsByTgId(message.getFrom().getId())) {
            showSubscribeChannel(message.getChatId(), null);
        }

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

                sendMessage(referrer.getTgId(),
                        String.format(
                                "🎉 По вашей реферальной ссылке зарегистрировался новый пользователь!\n" +
                                        "💰 С его покупок вы будете получать по 10%% на баланс\n\n" +
                                        "👤 Пользователь: %s (ID: %d)",
                                displayName != null ? displayName : "Новый пользователь",
                                user.getTgId()
                        )
                );
            }
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
        User user = userService.findOrCreateByTgId(userId);

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

        String text = """
        💰 *Пополнение баланса*
    
        Вы хотите пополнить баланс.
        Для этого выберите метод пополнения, а далее укажите сумму.
    
        Выберите способ оплаты:
        """;

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setReplyMarkup(keyboardFactory.getTopUpMenuInline());
                editMessage.setParseMode("Markdown");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getTopUpMenuInline());
                sendMessage.setParseMode("Markdown");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showAboutService(Long chatId, Integer messageId) {
        telegramUserService.updateState(chatId, BotState.INFO, BotState.START);

        String text = """
            <b>Юридическая информация</b>
            
            Используя наш сервис, вы подтверждаете, что ознакомились и соглашаетесь со следующими документами:
        
            • <a href="https://telegra.ph/Politika-konfidencialnosti-08-15-17" target="_blank">Политика конфиденциальности</a>
            
            • <a href="https://telegra.ph/Polzovatelskoe-soglashenie-08-15-10" target="_blank">Пользовательское соглашение</a>
            
            Продолжая пользоваться ботом и сервисом, вы принимаете условия указанных документов.
            """;

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setReplyMarkup(keyboardFactory.getInfoButton());
                editMessage.setDisableWebPagePreview(true);
                editMessage.setParseMode("HTML");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getInfoButton());
                sendMessage.setDisableWebPagePreview(true);
                sendMessage.setParseMode("HTML");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void checkPayment(Long chatId, Integer messageId, String transactionId, User user) {
        Optional<Payment> paymentOpt = paymentService.getPaymentByToken(transactionId);

        if (paymentOpt.isEmpty()) {
            String errorText = String.format("""
            ❌ <b>Платеж не найден</b>
    
            ID транзакции: <code>%s</code>
    
            Пожалуйста, проверьте данные или создайте новый платеж.
            """, transactionId);

            editMessageCaption(chatId, messageId, errorText, null);
            return;
        }

        Payment payment = paymentOpt.get();
        PaymentStatus lastStatus = PaymentStatus.fromString(payment.getStatus());

        if (!cooldownService.canCheck(chatId)) {
            long remaining = cooldownService.getRemainingCooldown(chatId);
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String fullText = String.format("""
    
            ⏰ <b>Проверка в %s</b>
    
            ⏳ <b>Слишком частые проверки</b>
    
            Проверять платеж можно раз в 10 секунд.
            Подождите ещё <b>%d секунд</b>.
            """, currentTime, remaining);

            editMessageCaption(chatId, messageId, fullText,
                    keyboardFactory.getPaymentCheckKeyboard(transactionId));
            return;
        }

        cooldownService.updateLastCheckTime(chatId);

        if (payment.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (!lastStatus.equals(PaymentStatus.EXPIRED)) {
                paymentService.markPaymentAsExpired(transactionId);
            }

            String expiredText = String.format("""
            ⌛️ <b>Срок платежа истек: %s</b>
    
            Платеж больше не действителен.
            Создайте новый платеж для пополнения.
            """.formatted(transactionId));

            editMessageCaption(chatId, messageId, expiredText, null);
            return;
        }

        String baseText = textFactory.checkPaymentText(payment);

        boolean isPaid = paymentService.checkAndConfirmPayment(payment);

        if (!isPaid) {
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String fullText = baseText + String.format("""
    
            ⏰ <b>Проверка в %s</b>
    
            ❌ <b>Платёж ещё не найден</b>
    
            Платеж еще не поступил или обрабатывается.
            Пожалуйста, попробуйте снова через 10 секунд.
            """, currentTime);

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
        String text = """
                ⌛️ <b>Срок платежа истек: %s</b>
        
                Платеж больше не действителен.
                Создайте новый платеж для пополнения.
                """.formatted(transactionId);

        SendMessage expiredMessage = new SendMessage();
        expiredMessage.setChatId(chatId.toString());
        expiredMessage.setText(text);
        expiredMessage.setParseMode("HTML");
        expiredMessage.setReplyMarkup(keyboardFactory.getBackButton());

        try {
            vpnBot.execute(expiredMessage);

            telegramUserService.updateState(chatId, BotState.PAYMENT_AWAITING_CONFIRMATION_CRYPTO,
                    BotState.BALANCE);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showSuccessPayment(Long chatId, Payment payment, User user) {
        String successText = textFactory.successText(payment, user);

        SendMessage successMessage = new SendMessage();
        successMessage.setChatId(chatId.toString());
        successMessage.setText(successText);
        successMessage.setParseMode("HTML");
        successMessage.setReplyMarkup(keyboardFactory.getBackButton());

        try {
            vpnBot.execute(successMessage);

            telegramUserService.updateState(chatId, BotState.PAYMENT_AWAITING_CONFIRMATION_CRYPTO,
                    BotState.BALANCE);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
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
                String expiryTime = Formatter.formatExpiryTime(cryptoPayment.getExpiresAt());

                String caption = String.format("""
                💸 <b>Пополнение баланса криптовалютой</b>
        
                💰 Сумма в рублях: <b>%s руб</b>
                💎 USDT: <code>%s</code> $
                📝 Memo: <code>%s</code>
        
                ⏳ <b>Действителен до:</b> %s (по мск)
        
                🔗 <b>Tonkeeper ссылка: </b>
                %s
        
                📱 <b>Инструкция:</b>
                1️⃣ Нажмите кнопку "🚀 Оплатить"
                2️⃣ Проверьте сумму и получателя
                3️⃣ Подтвердите транзакцию в кошельке
                4️⃣ Нажмите "Проверить оплату" ниже
        
                ⚠️ <b>Важно:</b> Убедитесь, что memo совпадает!
                """,
                        cryptoPayment.getAmountRub(),
                        cryptoPayment.getAmountUsdt(),
                        cryptoPayment.getTransactionId(),
                        expiryTime,
                        cryptoPayment.getTonLink()
                );

                showPhotoDirectly(chatId, qrBytes, caption, keyboardFactory.getPaymentCheckKeyboard(payment.getTransactionToken()));
            } else {
                telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT_CRYPTO, BotState.BALANCE);
                showErrorCreatePayment(chatId, user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showErrorCreatePayment(Long chatId, User user) {
        List<Payment> pendingPayments = paymentService.getUserPendingPayments(user.getId());
        int pendingCount = pendingPayments.size();

        String messageText = String.format("""
                    ⚠️ <b>Невозможно создать новый платёж</b>
            
                    У вас уже <b>%d из 5</b> возможных активных платежей.
            
                    🔴 <b>Что делать?</b>
                    • Оплатите один из существующих платежей
                    • Дождитесь истечения срока
            
                    👇 Нажмите кнопку для просмотра платежей
                    """, pendingCount);
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(messageText);
            sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
            sendMessage.setParseMode("HTML");
            vpnBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        } catch (Exception e) {
            e.printStackTrace();
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

        String text = String.format("""
        👋 Добро пожаловать в *NesVPN*, *%s*
        
        🔐 *Быстрый, безопасный и стабильный VPN для повседневного использования*
        
        ⚡️ *Преимущества:*
        _• Высокая скорость соединения_
        _• Работает в исключительных ситуациях_
        _• Низкая цена_
        _• Поддержка всех устройств_
        _• Бесплатный тестовый период_
        
        _Выберите действие в меню ниже_ 👇
        """, displayName != null ? displayName : "Дорогой друг");

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setReplyMarkup(keyboardFactory.getMainMenuInline());
                editMessage.setParseMode("Markdown");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getMainMenuInline());
                sendMessage.setParseMode("Markdown");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showProfile(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();

        telegramUserService.updateState(userId, BotState.PROFILE, BotState.START);

        String displayName = DisplayTelegramUsername.getDisplayName(vpnBot, user.getTgId());

        String referralLink = "https://t.me/" + vpnBot.getBotUsername() + "?start=" + user.getReferralCode();

        String text = String.format("""
        👤 *Ваш профиль*
        
        📛 _Имя:_ %s
        💰 _Баланс:_ %.2f₽
        👥 _Рефералов:_ %d
        📅 _Регистрация:_ %s
        
        🔗 *Ваша реферальная ссылка:*
        _%s_
        """,
                displayName,
                user.getBalance(),
                userService.getReferralsCount(user.getTgId()),
                user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Не указано",
                referralLink
        );

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                editMessage.setParseMode("Markdown");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                sendMessage.setParseMode("Markdown");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showReferrals(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();

        telegramUserService.updateState(userId, BotState.REFERRALS, BotState.START);

        String referralLink = "https://t.me/" + vpnBot.getBotUsername() + "?start=" + user.getReferralCode();

        List<User> referrals = userService.getReferralsByReferrer(userId);

        StringBuilder referralsList = new StringBuilder();
        BigDecimal totalEarnings = BigDecimal.ZERO;
        int referralsToShow = 15;
        int count = 0;

        for (User referral : referrals) {
            BigDecimal earnings = referralService.getReferralEarnings(user.getId(), referral.getId());
            totalEarnings = totalEarnings.add(earnings != null ? earnings : BigDecimal.ZERO);

            if (count < referralsToShow) {
                String referralUsername = DisplayTelegramUsername.getDisplayName(vpnBot,referral.getTgId());

                String refInfo = String.format(
                    "%d) %s (id: %d) принес %.2f₽",
                        count + 1,
                        referralUsername != null ? referralUsername : "no_username",
                        referral.getTgId(),
                        earnings != null ? earnings : BigDecimal.ZERO
                );
                referralsList.append(refInfo).append("\n");
                count++;
            }

        }

        if (referrals.size() > referralsToShow) {
            referralsList.append(String.format("... и еще %d", referrals.size() - referralsToShow));
        }

        String text = String.format("""
        <b>👥 Реферальная программа NesVPN</b>

        <b>💎 Как это работает:</b>
        • Каждый друг по вашей ссылке = <b>10%% от суммы</b> каждой покупки
        • Деньги с покупок рефералов <b>начисляются вам на баланс</b> автоматически
        • <b>Друг получит 50 рублей</b> на баланс
        • <b>Нет ограничений</b> по количеству приглашенных

        <b>🔗 Ваша ссылка:</b> %s

        <b>📊 Статистика:</b>
        👥 Рефералов: %d
        💰 Ваш доход: %.2f₽

        <b>📋 Ваши рефералы:</b>
        <blockquote expandable>%s</blockquote>
        """,
                referralLink,
                referrals.size(),
                totalEarnings,
                referrals.isEmpty() ? "Пока нет рефералов 😔" : referralsList.toString()
        );

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setParseMode("HTML");
                editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setParseMode("HTML");
                sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showInstructions(Long chatId, Integer messageId, User user) {
        Long userId = user.getTgId();
        telegramUserService.updateState(userId, BotState.INSTRUCTIONS, BotState.START);

        String text = """
        📖 *Инструкция по подключению NesVPN*

        *Общий процесс:*
        1. Скачай VPN-клиент на базе clash: Koala Clash, FlClashX или Clash Mi
        2. _Импорт подписки_ из Telegram
        3. Проверь _количество ссылкок_ в подписке
        4. _Подключиться_ ✅

        ⚠️ Иногда необходимо обновить подписку *2-3 раза* и *обязательно включить HWID * 

        Выберите вашу платформу для более подробной инструкции 👇
        """;

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setParseMode("Markdown");
                editMessage.setReplyMarkup(keyboardFactory.getInstructionsMenu());
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setParseMode("Markdown");
                sendMessage.setReplyMarkup(keyboardFactory.getInstructionsMenu());
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showAndroidInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_ANDROID, BotState.INSTRUCTIONS);

        String text = """
        📱 *Android — FlClashX клиен*

        _Шаг 1:_ Скачайте *FlClashX*
        🔗 [Скачать FlClashX из github](https://github.com/pluralplay/FlClashX/releases/download/v0.3.2/FlClashX-android-arm64-v8a.apk)

        _Шаг 2:_ Откройте Telegram → Скопируйте *ссылку на подписку*

        _Шаг 3:_ Откройте *FlClashX* → Вставьте ссылку по кнопке _Добавить профиль_

        _Шаг 4:_ Нажмите *"Подключиться"* ✅

        ⚠️ *Важно:* Проверьте, что конфиг импортировался в разделе _Профили_ и есть несколько серверов

        💡 *Проблемы?*
        • Обновите FlClashX до последней версии
        • Проверьте срок подписки и количество устройств в боте
        • Вставьте ссылку заново
        • Изучите ошибку или обратитесь в поддержку
        """;

        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode("Markdown");
            editMessage.disableWebPagePreview();
            editMessage.setReplyMarkup(keyboardFactory.getBackButton());
            vpnBot.execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showIosInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_IOS, BotState.INSTRUCTIONS);

        String text = """
        🔐 *iOS — Clash Mi*

        _Шаг 1:_ Скачайте *Clash Mi* из App Store
        🔗 [Скачать Clash Mi](https://apps.apple.com/us/app/clash-mi/id6744321968)

        _Шаг 2:_ Откройте Telegram → Скопируйте *ссылку на подписку*

        _Шаг 3:_ Откройте *Clash Mi*: *Вставьте ссылку в: Профили -> Нажмите на + -> Добавление подписки* и *ОБЯЗАТЕЛЬНО* выберите *"X-HWID"* - должен быть включен

        _Шаг 4:_ iOS запросит разрешение VPN → *"Разрешить"*

        _Шаг 5:_ Нажмите *"Подключиться"* на главной странице, менять сервера по кнопке: *Прокси* или *Панель* -> *NesVPN и дальше выбираете* ✅

        ⚠️ *Важно:* Проверьте, что конфиг импортировался в разделе _Профили_ и есть несколько серверов, пропингуйте их

        💡 *Проблемы?*
        • Проверьте срок подписки и количество устройств в боте
        • Вставьте ссылку заново
        • Изучите ошибку или обратитесь в поддержку
        """;

        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode("Markdown");
            editMessage.disableWebPagePreview();
            editMessage.setReplyMarkup(keyboardFactory.getBackButton());
            vpnBot.execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showWindowsInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_WINDOWS, BotState.INSTRUCTIONS);

        String text = """
        🌐 *Windows — Koala Clash*

        _Шаг 1:_ Скачайте *Koala Clash* для Windows
        🔗 [Скачать Koala Clash](https://github.com/coolcoala/clash-verge-rev-lite/releases/download/v0.2.10/Koala.Clash_x64-setup.exe)

        _Шаг 2:_ Откройте Telegram → Скопируйте *ссылку на подписку*

        _Шаг 3:_ Откройте *Koala Clash*: *Вставьте ссылку* в: Профили -> Нажмите на + и вставьте скопированную подписку

        _Шаг 4:_ Нажмите *"Подключиться"*, менять сервера по кнопке: *Прокси* -> *NesVPN* и дальше выбираете сервер или на *Главная* под кнопкой подключения ✅

        ⚠️ *Важно:* Проверьте, что конфиг импортировался в разделе _Прокси_ и есть несколько серверов, пропингуйте их

        💡 *Проблемы?*
        • Проверьте срок подписки и количество устройств в боте
        • Вставьте ссылку заново
        • Изучите ошибку или обратитесь в поддержку
        """;

        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode("Markdown");
            editMessage.disableWebPagePreview();
            editMessage.setReplyMarkup(keyboardFactory.getBackButton());
            vpnBot.execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showMacosInstructions(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.INSTRUCTIONS_MACOS, BotState.INSTRUCTIONS);

        String text = """
        🍎 *MacOS — Koala Clash*

        _Шаг 1:_ Скачайте *Koala Clash* для MacOS
        🔗 [Скачать Koala Clash apple silicon](https://github.com/coolcoala/clash-verge-rev-lite/releases/download/v0.2.10/Koala.Clash_aarch64.dmg)
        🔗 [Скачать Koala Clash intel](https://github.com/coolcoala/clash-verge-rev-lite/releases/download/v0.2.10/Koala.Clash_x64.dmg)

        _Шаг 2:_ Откройте Telegram → Скопируйте *ссылку на подписку*

        _Шаг 3:_ Откройте *Koala Clash*: *Вставьте ссылку* в: Профили -> Нажмите на + и вставьте скопированную подписку

        _Шаг 4:_ Нажмите *"Подключиться"*, менять сервера по кнопке: *Прокси* -> *NesVPN* и дальше выбираете сервер или на *Главная* под кнопкой подключения ✅

        ⚠️ *Важно:* Проверьте, что конфиг импортировался в разделе _Прокси_ и есть несколько серверов, пропингуйте их

        💡 *Проблемы?*
        • Проверьте срок подписки и количество устройств в боте
        • Вставьте ссылку заново
        • Изучите ошибку или обратитесь в поддержку
        """;

        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode("Markdown");
            editMessage.disableWebPagePreview();
            editMessage.setReplyMarkup(keyboardFactory.getBackButton());
            vpnBot.execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showBalance(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE, BotState.START);

        String text = String.format("""
        *💰 Ваш баланс*

        💵 *Текущий баланс:* %.2f₽

        _Баланс используется для:_
        • Оплаты VPN подписок
        • Продления активных подписок

        💡 _Пополняйте баланс и получайте бонусы за рефералов!_

        Выберите действие 👇
        """,
                user.getBalance()
        );


        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setParseMode("Markdown");
                editMessage.setReplyMarkup(keyboardFactory.getBalanceMenu());
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setParseMode("Markdown");
                sendMessage.setReplyMarkup(keyboardFactory.getBalanceMenu());
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showBalanceHistory(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_HISTORY, BotState.BALANCE);

        List<BalanceTransaction> history = balanceService.getHistory(user.getId());

        StringBuilder historyText = new StringBuilder();
        for (BalanceTransaction tx : history) {
            String sign = tx.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";

            historyText.append(String.format(
                    "<b>%s %s%.2f₽</b>\n<i>%s</i>\n%s\n\n",
                    tx.getType().getDisplayName(),
                    sign,
                    tx.getAmount(),
                    tx.getDescription() != null ? tx.getDescription() : "Без описания",
                    tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            ));
        }

        String text = String.format("""
        <b>📊 История операций</b>

        Последние 20 транзакций:
        
        %s
        """,
                history.isEmpty() ? "<i>История пуста, вы не совершили ни одной транзакции</i>" : "<blockquote expandable>" + historyText.toString() + "</blockquote>"
        );

        if (text.length() > 4096) {
            text = text.substring(0, 4090) + "\n...";
        }

        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(text);
            editMessage.setParseMode("HTML");
            editMessage.setReplyMarkup(keyboardFactory.getBackButton());
            vpnBot.execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showAwaitingBalanceDev(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_TOP_UP, BotState.BALANCE_TOP_UP);

        String text = """
        💰 *Пополнение баланса по СБП*
        
        Данный раздел находится в разработке, для пополнения *обратитесь к администратору* или используйте *другие способы оплаты*.
        """;

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                editMessage.setParseMode("Markdown");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                sendMessage.setParseMode("Markdown");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showAwaitingBalance(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT, BotState.BALANCE_TOP_UP);

        String text = """
        💰 *Пополнение баланса СБП*
        
        Введите сумму пополнения от *100₽* до *2000₽*
        
        Например: `500`
        """;

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                editMessage.setParseMode("Markdown");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                sendMessage.setParseMode("Markdown");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showAwaitingBalanceWithCrypto(Long chatId, Integer messageId, User user) {
        telegramUserService.updateState(user.getTgId(), BotState.BALANCE_AWAITING_AMOUNT_CRYPTO, BotState.BALANCE_TOP_UP);

        double rubRate = floatRatesService.getUsdToRubRate();
        String formattedRate = String.format("%.2f", rubRate);

        String text = """
        💰 *Пополнение баланса USDT (TON)*
        
        Введите сумму пополнения от *1$* до *25$*
        
        *Цена за 1 USDT:* %s руб.
        
        Например: `5`
        """.formatted(formattedRate);

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text);
                editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                editMessage.setParseMode("Markdown");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                sendMessage.setParseMode("Markdown");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showSubscribeChannel(Long chatId, Integer messageId) {
        String text = """
            📱 <b>Рекомендуем подписаться на канал</b>
            
            <i>💡 Чтобы быть в курсе всех новостей и анонсов, просим подписаться</i>
            """;

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text.trim());
                editMessage.setReplyMarkup(keyboardFactory.getSubscribeChannelKeyboard());
                editMessage.setParseMode("HTML");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getSubscribeChannelKeyboard());
                sendMessage.setParseMode("HTML");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showSubscription(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS, BotState.START);

        Token token = tokenService.getUserToken(user.getId());
        if (token == null) {
            String text = """
            📱 <b>Ваша подписка</b>
            
            У вас пока нет подписки.
            
            <i>💡 Обратитесь к администратору для получения подписки.</i>
            """;

            try {
                if (messageId != null) {
                    EditMessageText editMessage = new EditMessageText();
                    editMessage.setChatId(chatId);
                    editMessage.setMessageId(messageId);
                    editMessage.setText(text.trim());
                    editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                    editMessage.setParseMode("HTML");
                    vpnBot.execute(editMessage);
                } else {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(text);
                    sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                    sendMessage.setParseMode("HTML");
                    vpnBot.execute(sendMessage);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            long daysLeft = tokenService.getDaysLeft(token);
            boolean isActive = token.isActive();
            String tokenUrl = tokenService.getFullTokenUrl(token);

            String statusEmoji = isActive ? "✅" : "❌";
            String statusText = isActive ? "Активна" : "Истекла";

            String text = String.format("""
                        📱 <b>Ваша подписка</b>
                        
                        %s <b>Статус:</b> %s
                        
                        🔗 <b>Ссылка для подключения:</b>
                        <blockquote expandable><pre>%s</pre></blockquote>
                        
                        📅 <b>Действует до:</b> %s
                        ⏳ <b>Осталось дней:</b> %d
                        
                        👥 <b>Устройств всего:</b> 3%s
                        
                        <i></i>
                        """,
                    statusEmoji,
                    statusText,
                    tokenUrl,
                    token.getValidTo() != null
                            ? token.getValidTo()
                                .atZone(ZoneId.systemDefault())
                                .withZoneSameInstant(ZoneId.of("Europe/Moscow"))
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                            : "Не указано",
                    daysLeft,
                    daysLeft <= 7 && daysLeft > 0
                            ? "\n\n⚠️ <i>Срок подписки истекает скоро! Продлите её.</i>"
                            : ""
            );

            try {
                if (messageId != null) {
                    EditMessageText editMessage = new EditMessageText();
                    editMessage.setChatId(chatId);
                    editMessage.setMessageId(messageId);
                    editMessage.setText(text.trim());
                    editMessage.setReplyMarkup(keyboardFactory.getSubscriptionKeyboard(token.getId(), tokenUrl, token.isActive()));
                    editMessage.setParseMode("HTML");
                    vpnBot.execute(editMessage);
                } else {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(text);
                    sendMessage.setReplyMarkup(keyboardFactory.getSubscriptionKeyboard(token.getId(), tokenUrl, token.isActive()));
                    sendMessage.setParseMode("HTML");
                    vpnBot.execute(sendMessage);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }


    public void showSubscriptionExtend(Long chatId, Integer messageId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS_EXTEND, BotState.SUBSCRIPTIONS);

        Token token = tokenService.getUserToken(user.getId());

        if (token == null) {
            String text = """
            ❌ <b>Продление подписки</b>
            
            У вас нет активной подписки, вернитесь назад:
            """;

            try {
                if (messageId != null) {
                    EditMessageText editMessage = new EditMessageText();
                    editMessage.setChatId(chatId);
                    editMessage.setMessageId(messageId);
                    editMessage.setText(text.trim());
                    editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                    editMessage.setParseMode("HTML");
                    vpnBot.execute(editMessage);
                } else {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(text);
                    sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                    sendMessage.setParseMode("HTML");
                    vpnBot.execute(sendMessage);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        long daysLeft = tokenService.getDaysLeft(token);

        String text = String.format("""
            🔄 <b>Продление подписки</b>
            
            💳 <b>Ваш баланс:</b> %.2f₽
            
            📅 <b>Текущий срок действия:</b>
            %s
            
            ⏳ <b>Осталось дней:</b> %d
            
            💎 <b>Выберите срок продления:</b>
            Чем дольше срок, тем выгоднее цена!
            """,
                user.getBalance(),
                token.getValidTo() != null
                        ? token.getValidTo().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        : "Не указано",
                daysLeft
        );

        List<VpnPlan> plans = vpnPlanService.getAllPlans();

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text.trim());
                editMessage.setReplyMarkup(keyboardFactory.getExtendPlansKeyboard(token.getId(), plans));
                editMessage.setParseMode("HTML");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getExtendPlansKeyboard(token.getId(), plans));
                sendMessage.setParseMode("HTML");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showExtendConfirm(Long chatId, Integer messageId, Long tokenId, Long planId, User user) {
        Long tgId = user.getTgId();
        telegramUserService.updateState(tgId, BotState.SUBSCRIPTIONS_CONFIRM, BotState.SUBSCRIPTIONS_EXTEND);

        Token token = tokenService.findById(tokenId).orElse(null);
        VpnPlan plan = vpnPlanService.findById(planId).orElse(null);

        if (token == null || plan == null) {
            String text = """
                ❌ <b>Данные не найдены</b>
                
                Вернитесь назад:
                """;
            try {
                if (messageId != null) {
                    EditMessageText editMessage = new EditMessageText();
                    editMessage.setChatId(chatId);
                    editMessage.setMessageId(messageId);
                    editMessage.setText(text.trim());
                    editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                    editMessage.setParseMode("HTML");
                    vpnBot.execute(editMessage);
                } else {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(text);
                    sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                    sendMessage.setParseMode("HTML");
                    vpnBot.execute(sendMessage);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        LocalDateTime currentValidTo = token.getValidTo() != null ? token.getValidTo() : LocalDateTime.now();

        LocalDateTime baseDate = currentValidTo.isBefore(LocalDateTime.now())
                ? LocalDateTime.now()
                : currentValidTo;

        LocalDateTime newValidTo = baseDate.plusDays(plan.getDuration());

        String text = String.format("""
            ✅ <b>Подтверждение тарифа</b>
            
            📦 <b>Тариф:</b> %s
            💰 <b>Стоимость:</b> %d₽
            
            📅 <b>Текущий срок действия:</b>
            %s
            
            📅 <b>После продления:</b>
            %s
            
            💳 <b>Ваш баланс:</b> %.2f₽
            
            Подтвердите продление подписки
            """,
                plan.getName(),
                plan.getPrice(),
                currentValidTo.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                newValidTo.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                user.getBalance()
        );

        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text.trim());
                editMessage.setReplyMarkup(keyboardFactory.getConfirmExtendKeyboard(tokenId, planId));
                editMessage.setParseMode("HTML");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setReplyMarkup(keyboardFactory.getConfirmExtendKeyboard(tokenId, planId));
                sendMessage.setParseMode("HTML");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showExtendProcess(Long chatId, Integer messageId, Long tokenId, Long planId, User user) {
        Token token = tokenService.findById(tokenId).orElse(null);
        VpnPlan plan = vpnPlanService.findById(planId).orElse(null);

        if (token == null || plan == null) {
            String text = """
                ❌ <b>Данные не найдены</b>
                
                Вернитесь назад:
                """;
            try {
                if (messageId != null) {
                    EditMessageText editMessage = new EditMessageText();
                    editMessage.setChatId(chatId);
                    editMessage.setMessageId(messageId);
                    editMessage.setText(text.trim());
                    editMessage.setReplyMarkup(keyboardFactory.getBackButton());
                    editMessage.setParseMode("HTML");
                    vpnBot.execute(editMessage);
                } else {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(text);
                    sendMessage.setReplyMarkup(keyboardFactory.getBackButton());
                    sendMessage.setParseMode("HTML");
                    vpnBot.execute(sendMessage);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        if (user.getBalance().compareTo(BigDecimal.valueOf(plan.getPrice())) < 0) {
            String text = String.format("""
                ❌ <b>Недостаточно средств</b>
                
                <b>Стоимость:</b> %d₽
                <b>Ваш баланс:</b> %.2f₽
                
                Пополните баланс для продления подписки.
                """,
                    plan.getPrice(),
                    user.getBalance());

            try {
                if (messageId != null) {
                    EditMessageText editMessage = new EditMessageText();
                    editMessage.setChatId(chatId);
                    editMessage.setMessageId(messageId);
                    editMessage.setText(text.trim());
                    editMessage.setReplyMarkup(keyboardFactory.getBackToSubscriptionKeyboard());
                    editMessage.setParseMode("HTML");
                    vpnBot.execute(editMessage);
                } else {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(text);
                    sendMessage.setReplyMarkup(keyboardFactory.getBackToSubscriptionKeyboard());
                    sendMessage.setParseMode("HTML");
                    vpnBot.execute(sendMessage);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }


        String text = """
            ⏳ <b>Подписка скоро обновится</b>
            
            ✅ Вы успешно оплатили подписку!
            
            Ожидайте в течение пару минут,
            ваша ссылка обновится.
            """;

        Order order = orderService.createOrder(user.getId(), plan);
        log.info("Заказ {} cоздан", order.getId());
        try {
            if (messageId != null) {
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.setText(text.trim());
                editMessage.setParseMode("HTML");
                vpnBot.execute(editMessage);
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(text);
                sendMessage.setParseMode("HTML");
                vpnBot.execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        showStart(chatId, null, user);

    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            vpnBot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);
        try {
            vpnBot.execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
