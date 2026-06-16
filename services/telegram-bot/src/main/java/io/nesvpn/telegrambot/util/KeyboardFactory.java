package io.nesvpn.telegrambot.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.dto.HwidDevice;
import io.nesvpn.telegrambot.enums.PaymentMethod;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.model.VpnPlan;
import io.nesvpn.telegrambot.services.OrderService;
import io.nesvpn.telegrambot.services.PaymentService;
import io.nesvpn.telegrambot.services.TonPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class KeyboardFactory {
    private static final String STYLE_PRIMARY = "primary";
    private static final String STYLE_SUCCESS = "success";
    private static final String STYLE_DANGER = "danger";

    private enum ButtonIcon {
        PROFILE("5258362837411045098"),
        SUBSCRIPTION("5359629206948976159"),
        BALANCE("5258204546391351475"),
        REFERRALS("5258513401784573443"),
        INFO("5258503720928288433"),
        CHANNEL("5260268501515377807"),
        PAYMENT("5258368777350816286"),
        REFRESH("5258420634785947640"),
        CRYPTO("5359719332542718652"),
        DOCUMENT("5258477770735885832"),
        BOOK("5260512129240276089"),
        SUPPORT("5258215846450305872"),
        BACK("5258236805890710909"),
        DEVICE("5258423306255604960"),
        DELETE("5258130763148172425"),
        HOME("5258084656674250503"),
        ANDROID("5258093637450866522"),
        LOCK("5258476306152038031"),
        CHART("5258391025281408576"),
        PACKAGE("5258134813302332906"),
        SUCCESS("5260726538302660868"),
        LUCKY("5258508428212445001"),
        BROADCAST("5258477770735885832"),
        SPIN("5258165702707125574"),
        PLUS("5258108352008823107");

        private final String customEmojiId;

        ButtonIcon(String customEmojiId) {
            this.customEmojiId = customEmojiId;
        }
    }

    private static class IconInlineKeyboardButton extends InlineKeyboardButton {
        private String iconCustomEmojiId;
        private String style;

        @JsonProperty("icon_custom_emoji_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getIconCustomEmojiId() {
            return iconCustomEmojiId;
        }

        public void setIconCustomEmojiId(String iconCustomEmojiId) {
            this.iconCustomEmojiId = iconCustomEmojiId;
        }

        @JsonProperty("style")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }
    }

    private static class IconKeyboardButton extends KeyboardButton {
        private String iconCustomEmojiId;
        private String style;

        @JsonProperty("icon_custom_emoji_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getIconCustomEmojiId() {
            return iconCustomEmojiId;
        }

        public void setIconCustomEmojiId(String iconCustomEmojiId) {
            this.iconCustomEmojiId = iconCustomEmojiId;
        }

        @JsonProperty("style")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }
    }

    private final PaymentService paymentService;
    private final TonPaymentService tonPaymentService;
    private final OrderService orderService;

    @Value("${bot.channel.username}")
    private String channelUsername;

    @Value("${bot.support}")
    private String support;

    @Value("${platega.pay-url}")
    private String plategaPayUrl;

    @Value("${platega.merchant-id}")
    private String merchantId;

    public KeyboardFactory(PaymentService paymentService, TonPaymentService tonPaymentService, OrderService orderService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.tonPaymentService = tonPaymentService;
    }

    private InlineKeyboardButton inlineButton(String text, ButtonIcon icon) {
        IconInlineKeyboardButton button = new IconInlineKeyboardButton();
        button.setText(text);
        button.setIconCustomEmojiId(icon.customEmojiId);
        return button;
    }

    private InlineKeyboardButton inlineButton(String text, ButtonIcon icon, String style) {
        IconInlineKeyboardButton button = new IconInlineKeyboardButton();
        button.setText(text);
        button.setIconCustomEmojiId(icon.customEmojiId);
        button.setStyle(style);
        return button;
    }

    private KeyboardButton keyboardButton(String text, ButtonIcon icon) {
        IconKeyboardButton button = new IconKeyboardButton();
        button.setText(text);
        button.setIconCustomEmojiId(icon.customEmojiId);
        return button;
    }

    private KeyboardButton keyboardButton(String text, ButtonIcon icon, String style) {
        IconKeyboardButton button = new IconKeyboardButton();
        button.setText(text);
        button.setIconCustomEmojiId(icon.customEmojiId);
        button.setStyle(style);
        return button;
    }

    public InlineKeyboardMarkup getMainMenuInline() {
        return getMainMenuInline(false);
    }

    public InlineKeyboardMarkup getMainMenuInline(boolean canBroadcast) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton profileBtn = inlineButton("Профиль", ButtonIcon.PROFILE, STYLE_SUCCESS);
        profileBtn.setCallbackData("profile");
        row1.add(profileBtn);

        InlineKeyboardButton subscriptionsBtn = inlineButton("Подписка", ButtonIcon.SUBSCRIPTION, STYLE_PRIMARY);
        subscriptionsBtn.setCallbackData("subscription");
        row1.add(subscriptionsBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton balanceBtn = inlineButton("Баланс", ButtonIcon.BALANCE);
        balanceBtn.setCallbackData("balance");
        row2.add(balanceBtn);

        InlineKeyboardButton referralBtn = inlineButton("Рефералы", ButtonIcon.REFERRALS);
        referralBtn.setCallbackData("referrals");
        row2.add(referralBtn);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton infoBtn = inlineButton("О сервисe", ButtonIcon.INFO);
        infoBtn.setCallbackData("info");
        row3.add(infoBtn);
        if (canBroadcast) {
            InlineKeyboardButton broadcastBtn = inlineButton("Рассылка", ButtonIcon.BROADCAST, STYLE_PRIMARY);
            broadcastBtn.setCallbackData("broadcast");
            row3.add(broadcastBtn);
        }
        rows.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton channelBtn = inlineButton("Наш канал", ButtonIcon.CHANNEL);
        channelBtn.setUrl("https://t.me/nesvpn");

        row4.add(channelBtn);
        rows.add(row4);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getPaymentCheckKeyboard(String transactionId) {
        if (transactionId == null || transactionId.isEmpty()) {
            return null;
        }

        Optional<Payment> paymentOpt = paymentService.getPaymentByToken(transactionId);

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();

            PaymentMethod paymentMethod = PaymentMethod.valueOf(payment.getMethod());

            if (paymentMethod.equals(PaymentMethod.CRYPTO)) {
                return getPaymentCheckCryptoKeyboard(payment);
            } else if (paymentMethod.equals(PaymentMethod.SBP)) {
                return getPaymentCheckSbpKeyboard(payment);
            }
        }

        return null;
    }

    public InlineKeyboardMarkup getPaymentCheckSbpKeyboard(Payment payment) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String url = plategaPayUrl + "?id=" + payment.getTransactionToken() + "&mh=" + merchantId;

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton payButton = inlineButton("Оплатить", ButtonIcon.PAYMENT, STYLE_PRIMARY);
        payButton.setUrl(url);
        row1.add(payButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton checkButton = inlineButton("Проверить оплату", ButtonIcon.REFRESH, STYLE_SUCCESS);
        checkButton.setCallbackData("check_payment_sbp" + payment.getTransactionToken());
        row2.add(checkButton);
        rows.add(row2);

        markup.setKeyboard(rows);

        return markup;
    }

    public InlineKeyboardMarkup getTopUpMenuInline() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton sbpButton = inlineButton("По СБП", ButtonIcon.PAYMENT);
        sbpButton.setCallbackData("payment_method_sbp");
        row1.add(sbpButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton cryptoButton = inlineButton("USDT (Ton)", ButtonIcon.CRYPTO);
        cryptoButton.setCallbackData("payment_method_usdt");
        row2.add(cryptoButton);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backButton = inlineButton("Назад", ButtonIcon.BACK);
        backButton.setCallbackData("back");
        row3.add(backButton);
        rows.add(row3);

        markup.setKeyboard(rows);

        return markup;
    }

    public InlineKeyboardMarkup getPaymentCheckCryptoKeyboard(Payment payment) {
        CryptoPayment cryptoPayment = tonPaymentService.createUsdtPayment(payment);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String tonLink = cryptoPayment.getTonLink();
        if (tonLink != null) {
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton sendButton = inlineButton("Оплатить", ButtonIcon.PAYMENT, STYLE_PRIMARY);
            sendButton.setUrl(tonLink);
            row1.add(sendButton);
            rows.add(row1);
        }


        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton checkButton = inlineButton("Проверить оплату", ButtonIcon.REFRESH, STYLE_SUCCESS);
        checkButton.setCallbackData("check_payment_crypto_" + payment.getTransactionToken());
        row2.add(checkButton);
        rows.add(row2);

        keyboard.setKeyboard(rows);

        return keyboard;
    }

    public InlineKeyboardMarkup getSubscribeChannelKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String channelLink = "https://t.me/" + channelUsername;

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton titleButton = inlineButton("Подписаться на канал", ButtonIcon.CHANNEL);
        titleButton.setUrl(channelLink);
        row1.add(titleButton);

        rows.add(row1);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public InlineKeyboardMarkup getInfoButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton privacyButton = inlineButton("Политика конфиденциальности", ButtonIcon.DOCUMENT);
        privacyButton.setUrl("https://telegra.ph/Politika-konfidencialnosti-04-01-26");
        row1.add(privacyButton);

        InlineKeyboardButton agreementButton = inlineButton("Пользовательское соглашение", ButtonIcon.BOOK);
        agreementButton.setUrl("https://telegra.ph/Polzovatelskoe-soglashenie-04-01-19");
        row1.add(agreementButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton supportButton = inlineButton("Поддержка NesVPN", ButtonIcon.SUPPORT);
        supportButton.setUrl("t.me/" + support);
        row2.add(supportButton);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        row3.add(backBtn);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getBackButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        row.add(backBtn);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getBroadcastAwaitingPostKeyboard() {
        return getBackButton();
    }

    public InlineKeyboardMarkup getBroadcastProgressKeyboard(Long campaignId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton refreshBtn = inlineButton("Обновить", ButtonIcon.REFRESH, STYLE_SUCCESS);
        refreshBtn.setCallbackData("broadcast_refresh_" + campaignId);
        row.add(refreshBtn);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getBroadcastHomeKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton startBtn = inlineButton("Главная", ButtonIcon.HOME, STYLE_SUCCESS);
        startBtn.setCallbackData("broadcast_home");
        row.add(startBtn);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getHwidDevicesKeyboard(List<HwidDevice> hwidDevices) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        hwidDevices.forEach(device -> {
            List<InlineKeyboardButton> row = new ArrayList<>();

            String model = device.getDeviceModel() != null
                    ? device.getDeviceModel()
                    : "Устройство";

            String date = device.getCreatedAt() != null
                    ? Formatter.formatMoscow(device.getCreatedAt().toLocalDateTime(), "dd.MM")
                    : "";

            String deviceClient = device.getUserAgent().toLowerCase().replace(" ", "");
            String clientText = null;
            if (deviceClient.contains("happ")) {
                clientText = "Happ";
            } else if (deviceClient.contains("flclashx")) {
                clientText = "Flclash X";
            } else if (deviceClient.contains("koala-clash")) {
                clientText = "Koala Clash";
            }

            int MAX_LEN = 64;
            String text = (clientText != null ? clientText + " • " : "") + model + " • " + date;

            if (text.length() > MAX_LEN) {
                text = text.substring(0, MAX_LEN);
            }

            IconInlineKeyboardButton deviceBtn = new IconInlineKeyboardButton();
            deviceBtn.setText(text);
            deviceBtn.setIconCustomEmojiId(ButtonIcon.DEVICE.customEmojiId);
            deviceBtn.setCallbackData("delete_hwid_confirm_" + device.getHwid());

            row.add(deviceBtn);
            rows.add(row);
        });

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        row.add(backBtn);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getHwidDeviceDeleteKeyboard(String hwid) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        row.add(backBtn);

        InlineKeyboardButton deleteBtn = inlineButton("Удалить", ButtonIcon.DELETE, STYLE_DANGER);
        deleteBtn.setCallbackData("delete_hwid_confirmation_" + hwid);
        row.add(deleteBtn);

        rows.add(row);
        markup.setKeyboard(rows);

        return markup;
    }

    public InlineKeyboardMarkup getExpiringSubscriptionMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton subscriptionsBtn = inlineButton("Подписка", ButtonIcon.SUBSCRIPTION, STYLE_PRIMARY);
        subscriptionsBtn.setCallbackData("subscription");
        row1.add(subscriptionsBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton startBtn = inlineButton("Главное меню", ButtonIcon.HOME);
        startBtn.setCallbackData("start");
        row2.add(startBtn);
        rows.add(row2);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getInstructionsMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton androidBtn = inlineButton("Android", ButtonIcon.ANDROID);
        androidBtn.setCallbackData("instructions_android");
        row1.add(androidBtn);

        InlineKeyboardButton iosBtn = inlineButton("iOS", ButtonIcon.LOCK);
        iosBtn.setCallbackData("instructions_ios");
        row1.add(iosBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton windowsBtn = inlineButton("Windows", ButtonIcon.DEVICE);
        windowsBtn.setCallbackData("instructions_windows");
        row2.add(windowsBtn);

        InlineKeyboardButton macosBtn = inlineButton("MacOS", ButtonIcon.DEVICE);
        macosBtn.setCallbackData("instructions_macos");
        row2.add(macosBtn);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        row3.add(backBtn);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getBalanceMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton historyBtn = inlineButton("История операций", ButtonIcon.CHART);
        historyBtn.setCallbackData("balance_history");
        row1.add(historyBtn);

        InlineKeyboardButton topupBtn = inlineButton("Пополнить", ButtonIcon.PAYMENT, STYLE_PRIMARY);
        topupBtn.setCallbackData("balance_topup");
        row1.add(topupBtn);

        rows.add(row1);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        row3.add(backBtn);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getSubscriptionKeyboard(boolean isActive, Integer devicesCount, boolean autoRenewalEnabled) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton extendBtn = inlineButton(isActive ? "Продлить" : "Возобновить", ButtonIcon.REFRESH, STYLE_PRIMARY);
        extendBtn.setCallbackData("subscription_extend");
        row1.add(extendBtn);

        String devicesButtonText = devicesCount != null
                ? String.format("Устройства (%d)", devicesCount)
                : "Устройства";
        InlineKeyboardButton devicesBtn = inlineButton(devicesButtonText, ButtonIcon.DEVICE);
        devicesBtn.setCallbackData("subscription_devices");
        row1.add(devicesBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton autoRenewalBtn = inlineButton(
                "Автопродление: " + (autoRenewalEnabled ? "ВКЛ" : "ВЫКЛ"),
                ButtonIcon.PAYMENT,
                autoRenewalEnabled ? STYLE_SUCCESS : STYLE_DANGER
        );
        autoRenewalBtn.setCallbackData("subscription_auto_renewal_toggle");
        row2.add(autoRenewalBtn);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton luckyBtn = inlineButton("Попытать удачу", ButtonIcon.LUCKY, STYLE_PRIMARY);
        luckyBtn.setCallbackData("subscription_lucky_777");
        row3.add(luckyBtn);
        rows.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton instructionBtn = inlineButton("Инструкция", ButtonIcon.BOOK);
        instructionBtn.setCallbackData("instructions");
        row4.add(instructionBtn);
        rows.add(row4);

        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        row5.add(backBtn);
        rows.add(row5);

        markup.setKeyboard(rows);
        return markup;
    }

    public ReplyKeyboardMarkup getLucky777ReplyKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);
        markup.setSelective(true);

        KeyboardRow row = new KeyboardRow();
        row.add(keyboardButton("Назад", ButtonIcon.BACK));
        row.add(keyboardButton("🎰", ButtonIcon.SPIN, STYLE_SUCCESS));

        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row);
        markup.setKeyboard(rows);

        return markup;
    }

    public ReplyKeyboardRemove getRemoveReplyKeyboard() {
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setRemoveKeyboard(true);
        return remove;
    }

    public InlineKeyboardMarkup getSubscriptionKeyboardFirst(UUID userId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton extendBtn = inlineButton("Получить 2 дня бесплатно", ButtonIcon.PLUS, STYLE_SUCCESS);
        extendBtn.setCallbackData("get_2_days_free_" + userId);
        row1.add(extendBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton instructionBtn = inlineButton("Инструкция", ButtonIcon.BOOK);
        instructionBtn.setCallbackData("instructions");
        row2.add(instructionBtn);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        row3.add(backBtn);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }


    public InlineKeyboardMarkup getExtendPlansKeyboard(Long tokenId, List<VpnPlan> plans) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (VpnPlan plan : plans) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton planBtn = inlineButton(String.format("%s — %d₽", plan.getName(), plan.getPrice()), ButtonIcon.PACKAGE);
            planBtn.setCallbackData("extend_confirm_" + tokenId + "_" + plan.getId());
            row.add(planBtn);
            rows.add(row);
        }

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад", ButtonIcon.BACK);
        backBtn.setCallbackData("back");
        backRow.add(backBtn);
        rows.add(backRow);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getConfirmExtendKeyboard(Long tokenId, Long planId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton confirmBtn = inlineButton("Подтвердить продление", ButtonIcon.SUCCESS, STYLE_SUCCESS);
        confirmBtn.setCallbackData("extend_process_" + tokenId + "_" + planId);
        row1.add(confirmBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton cancelBtn = inlineButton("Назад", ButtonIcon.BACK);
        cancelBtn.setCallbackData("back");
        row2.add(cancelBtn);
        rows.add(row2);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getBackToSubscriptionKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backBtn = inlineButton("Назад к подписке", ButtonIcon.BACK);
        backBtn.setCallbackData("subscription");
        row.add(backBtn);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }
}
