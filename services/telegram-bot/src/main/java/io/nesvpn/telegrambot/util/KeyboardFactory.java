package io.nesvpn.telegrambot.util;

import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.model.VpnPlan;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class KeyboardFactory {

    public InlineKeyboardMarkup getMainMenuInline() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton profileBtn = new InlineKeyboardButton();
        profileBtn.setText("👤 Профиль");
        profileBtn.setCallbackData("profile");
        row1.add(profileBtn);

        InlineKeyboardButton subscriptionsBtn = new InlineKeyboardButton();
        subscriptionsBtn.setText("📱 Подписка");
        subscriptionsBtn.setCallbackData("subscription");
        row1.add(subscriptionsBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton balanceBtn = new InlineKeyboardButton();
        balanceBtn.setText("💰 Баланс");
        balanceBtn.setCallbackData("balance");
        row2.add(balanceBtn);

        InlineKeyboardButton referralBtn = new InlineKeyboardButton();
        referralBtn.setText("👥 Рефералы");
        referralBtn.setCallbackData("referrals");
        row2.add(referralBtn);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton instructionBtn = new InlineKeyboardButton();
        instructionBtn.setText("📖 Инструкция");
        instructionBtn.setCallbackData("instructions");
        row3.add(instructionBtn);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getPaymentMenuInline(String transactionId, Integer amount) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton payButton = new InlineKeyboardButton();
        payButton.setText("💳 Оплатить");
        payButton.setUrl("https://example.com/pay/" + transactionId);
        row1.add(payButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton checkButton = new InlineKeyboardButton();
        checkButton.setText("🔄 Проверить оплату");
        checkButton.setCallbackData("check_payment_sbp" + transactionId + "_" + amount);
        row2.add(checkButton);
        rows.add(row2);

        markup.setKeyboard(rows);

        return markup;
    }

    public InlineKeyboardMarkup getTopUpMenuInline() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton sbpButton = new InlineKeyboardButton();
        sbpButton.setText("💳 По СБП");
        sbpButton.setCallbackData("payment_method_sbp");
        row1.add(sbpButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton cryptoButton = new InlineKeyboardButton();
        cryptoButton.setText("\uD83D\uDCB2 USDT (Ton)");
        cryptoButton.setCallbackData("payment_method_usdt");
        row2.add(cryptoButton);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("back");
        row3.add(backButton);
        rows.add(row3);

        markup.setKeyboard(rows);

        return markup;
    }

    public InlineKeyboardMarkup getPaymentCheckCryptoKeyboard(String transactionId, String tonLink) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (tonLink != null) {
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton sendButton = new InlineKeyboardButton();
            sendButton.setText("\uD83D\uDE80 Оплатить");
            sendButton.setUrl(tonLink);
            row1.add(sendButton);
            rows.add(row1);
        }


        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton checkButton = new InlineKeyboardButton();
        checkButton.setText("🔄 Проверить оплату");
        checkButton.setCallbackData("check_payment_crypto_" + transactionId);
        row2.add(checkButton);
        rows.add(row2);

        keyboard.setKeyboard(rows);

        return keyboard;
    }

    public InlineKeyboardMarkup getBackButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("◀️ Назад");
        backBtn.setCallbackData("back");
        row.add(backBtn);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getInstructionsMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton androidBtn = new InlineKeyboardButton();
        androidBtn.setText("📱 Android");
        androidBtn.setCallbackData("instructions_android");
        row1.add(androidBtn);

        InlineKeyboardButton iosBtn = new InlineKeyboardButton();
        iosBtn.setText("🔐 iOS");
        iosBtn.setCallbackData("instructions_ios");
        row1.add(iosBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton windowsBtn = new InlineKeyboardButton();
        windowsBtn.setText("🌐 Windows");
        windowsBtn.setCallbackData("instructions_windows");
        row2.add(windowsBtn);

        InlineKeyboardButton macosBtn = new InlineKeyboardButton();
        macosBtn.setText("🍎 MacOS");
        macosBtn.setCallbackData("instructions_macos");
        row2.add(macosBtn);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("◀️ Назад");
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
        InlineKeyboardButton historyBtn = new InlineKeyboardButton();
        historyBtn.setText("📊 История операций");
        historyBtn.setCallbackData("balance_history");
        row1.add(historyBtn);

        InlineKeyboardButton topupBtn = new InlineKeyboardButton();
        topupBtn.setText("💳 Пополнить");
        topupBtn.setCallbackData("balance_topup");
        row1.add(topupBtn);

        rows.add(row1);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("◀️ Назад");
        backBtn.setCallbackData("back");
        row3.add(backBtn);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getSubscriptionKeyboard(Long tokenId, String tokenUrl, boolean isActive) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton extendBtn = new InlineKeyboardButton();
        extendBtn.setText(isActive ? "🔄 Продлить подписку" : "🔄 Возобновить подписку");
        extendBtn.setCallbackData("subscription_extend");
        row1.add(extendBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("◀️ Назад");
        backBtn.setCallbackData("back");
        row2.add(backBtn);
        rows.add(row2);

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getExtendPlansKeyboard(Long tokenId, List<VpnPlan> plans) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (VpnPlan plan : plans) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton planBtn = new InlineKeyboardButton();
            planBtn.setText(String.format("📦 %s — %d₽", plan.getName(), plan.getPrice()));
            planBtn.setCallbackData("extend_confirm_" + tokenId + "_" + plan.getId());
            row.add(planBtn);
            rows.add(row);
        }

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("◀️ Назад");
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
        InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
        confirmBtn.setText("✅ Подтвердить продление");
        confirmBtn.setCallbackData("extend_process_" + tokenId + "_" + planId);
        row1.add(confirmBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("◀️ Назад");
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
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("◀️ Назад к подписке");
        backBtn.setCallbackData("subscription");
        row.add(backBtn);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }
}
