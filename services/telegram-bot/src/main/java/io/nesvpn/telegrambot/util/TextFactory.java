package io.nesvpn.telegrambot.util;

import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.enums.PaymentMethod;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.PaymentService;
import io.nesvpn.telegrambot.services.TonPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class TextFactory {
    @Value("${platega.pay-url}")
    private String plategaPayUrl;

    @Value("${platega.merchant-id}")
    private String merchantId;

    private final TonPaymentService tonPaymentService;

    public TextFactory(TonPaymentService tonPaymentService, PaymentService paymentService) {
        this.tonPaymentService = tonPaymentService;
    }

    public String checkPaymentText(Payment payment) {
        if (payment.getMethod().equals(PaymentMethod.CRYPTO.getValue())) {
            return getPaymentTextCrypto(payment);
        } else if (payment.getMethod().equals(PaymentMethod.SBP.getValue())) {
            return getPaymentTextSbp(payment);
        }

        return "";
    }

    public String getPaymentTextCrypto(Payment payment) {
        CryptoPayment cryptoPayment = tonPaymentService.createUsdtPayment(payment);
        String expiryTime = Formatter.formatExpiryTime(cryptoPayment.getExpiresAt());

        return String.format("""
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
                cryptoPayment.getTonLink());
    }

    public String getPaymentTextSbp(Payment payment) {
        CryptoPayment cryptoPayment = tonPaymentService.createUsdtPayment(payment);
        String expiryTime = Formatter.formatExpiryTime(cryptoPayment.getExpiresAt());

        return String.format("""
        💸 <b>Пополнение баланса по СБП</b>

        💰 Сумма в рублях: <b>%.2f ₽</b>
        🆔 ID транзакции: <code>%s</code>
        
        ⏳ <b>Действителен до:</b> %s (по мск)

        🔗 <b>Ссылка на платежную систему: </b>
        %s

        📱 <b>Инструкция:</b>
        1️⃣ Нажмите кнопку "Оплатить" ниже.
        2️⃣ Следуйте указаниям платежной системы.
        3️⃣ После оплаты вернитесь к боту и нажмите "Проверить оплату"
        """,
                payment.getAmount().setScale(2, RoundingMode.HALF_UP).doubleValue(),
                payment.getTransactionToken(),
                expiryTime,
                plategaPayUrl + "?id=" + payment.getTransactionToken() + "&mh=" + merchantId
        );
    }

    public String successText(Payment payment, User user) {
        // тк в payment храним в долларах
        if (payment.getMethod().equals(PaymentMethod.CRYPTO.getValue())) {
            CryptoPayment cryptoPayment = tonPaymentService.createUsdtPayment(payment);

            return String.format("""
            ✅ <b>Оплата подтверждена!</b>
            
            💰 Ваш баланс пополнен на <b>%s ₽</b>
            💎 Сумма в USDT: <b>%s ₽</b>
            📊 Текущий баланс: <b>%.2f ₽</b>
            
            Спасибо за использование нашего сервиса!
            """,
                  cryptoPayment.getAmountRub(),
                  cryptoPayment.getAmountUsdt(),
                  user.getBalance());
        } else if (payment.getMethod().equals(PaymentMethod.SBP.getValue())) {
            return String.format("""
            ✅ <b>Оплата подтверждена!</b>
            
            💰 Ваш баланс пополнен на <b>%s ₽</b>
            📊 Текущий баланс: <b>%.2f ₽</b>
            
            Спасибо за использование нашего сервиса!
            """,
                    payment.getAmount(),
                    user.getBalance());
        }

        return "";
    }
}
