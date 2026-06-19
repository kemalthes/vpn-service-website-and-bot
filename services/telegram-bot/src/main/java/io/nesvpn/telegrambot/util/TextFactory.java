package io.nesvpn.telegrambot.util;

import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.dto.HwidDevice;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastProgress;
import io.nesvpn.telegrambot.dto.broadcast.BroadcastStats;
import io.nesvpn.telegrambot.enums.BroadcastCampaignStatus;
import io.nesvpn.telegrambot.enums.PaymentMethod;
import io.nesvpn.telegrambot.enums.SubscriptionExpirationNotificationType;
import io.nesvpn.telegrambot.model.BalanceTransaction;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.TonPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TextFactory {
    private static final String TEXT_CHECK = customEmoji("5426920272051577496", "✅");
    private static final String TEXT_ERROR = customEmoji("5168125810745279194", "❌");
    private static final String TEXT_WARN = customEmoji("5168125810745279194", "⚠️");
    private static final String TEXT_WAIT = customEmoji("5176930085680185916", "⏳");
    private static final String TEXT_MONEY = customEmoji("5172639606625010326", "💰");
    private static final String TEXT_PAYMENT = customEmoji("5407082565435663265", "💳");
    private static final String TEXT_USDT = customEmoji("5168326806624797307", "💎");
    private static final String TEXT_NOTE = customEmoji("5172506385329422865", "📝");
    private static final String TEXT_LINK = customEmoji("5172769263097741912", "🔗");
    private static final String TEXT_DEVICE = customEmoji("5177440211830833975", "📱");
    private static final String TEXT_LOCK = customEmoji("5174666238483235441", "🔐");
    private static final String TEXT_BULB = customEmoji("5170151231422726790", "💡");
    private static final String TEXT_PACKAGE = customEmoji("5172759358903157807", "📦");
    private static final String TEXT_REFRESH = customEmoji("5170202955713872686", "🔄");
    private static final String TEXT_PEOPLE = customEmoji("5172412845236683692", "👥");
    private static final String TEXT_PROFILE = customEmoji("5170423021248184934", "👤");
    private static final String TEXT_NAME = customEmoji("5335073603312428316", "🏷️");
    private static final String TEXT_DATE = customEmoji("5350813266882870124", "📅");
    private static final String TEXT_CHART = customEmoji("5170311133055156754", "📊");
    private static final String TEXT_BOOK = customEmoji("5172506385329422865", "📖");
    private static final String TEXT_POINT_DOWN = customEmoji("5172653165836763657", "👇");
    private static final String TEXT_WAVE = customEmoji("5170203290721321766", "👋");
    private static final String TEXT_LUCKY = customEmoji("5172506368149553836", "🤩");
    private static final String TEXT_PARTY = customEmoji("5172632361015182081", "🥳");
    private static final String TEXT_GIFT = customEmoji("5170162552956519052", "🎉");
    private static final String TEXT_STAR = customEmoji("5170202955713872686", "🔥");
    private static final String TEXT_ALERT = customEmoji("5172803236289053353", "😱");
    private static final String TEXT_NO_WIN = customEmoji("5170462573602013949", "😢");
    private static final String TEXT_AUTO_RENEWAL = customEmoji("5368324170671202286", "⏰");
    private static final String SLOT_DICE = "<code>🎰</code>";

    @Value("${platega.pay-url}")
    private String plategaPayUrl;

    @Value("${platega.merchant-id}")
    private String merchantId;

    @Value("${project.referral-percent}")
    private String referralPercent;

    @Value("${project.max-devices}")
    private Integer maxDevices;

    private final TonPaymentService tonPaymentService;

    public TextFactory(TonPaymentService tonPaymentService) {
        this.tonPaymentService = tonPaymentService;
    }

    private static String customEmoji(String customEmojiId, String fallbackEmoji) {
        return "<tg-emoji emoji-id=\"" + customEmojiId + "\">" + fallbackEmoji + "</tg-emoji>";
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
        %s <b>Пополнение баланса криптовалютой</b>
        
        Сумма в рублях: <b>%s руб</b>
        USDT: <code>%s</code> $
        Address: <code>%s</code>
        %s Memo: <code>%s</code>

        %s <b>Действителен до:</b> %s (по мск)

        %s <b>Tonkeeper ссылка: </b>
        %s

        %s <b>Инструкция:</b>
        1. Нажмите кнопку "Оплатить"
        2. Проверьте сумму и получателя
        3. Подтвердите транзакцию в кошельке
        4. Нажмите "Проверить оплату" ниже

        %s <b>Важно:</b> Убедитесь, что memo совпадает!
        """,
                TEXT_USDT,
                cryptoPayment.getAmountRub(),
                cryptoPayment.getAmountUsdt(),
                cryptoPayment.getWalletAddress(),
                TEXT_NOTE,
                cryptoPayment.getTransactionId(),
                TEXT_WAIT,
                expiryTime,
                TEXT_LINK,
                cryptoPayment.getTonLink(),
                TEXT_DEVICE,
                TEXT_WARN);
    }

    public String getPaymentTextSbp(Payment payment) {
        CryptoPayment cryptoPayment = tonPaymentService.createUsdtPayment(payment);
        String expiryTime = Formatter.formatExpiryTime(cryptoPayment.getExpiresAt());

        return String.format("""
        %s <b>Пополнение баланса по СБП</b>

        Сумма в рублях: <b>%.2f ₽</b>
        ID транзакции: <code>%s</code>
        
        %s <b>Действителен до:</b> %s (по мск)

        %s <b>Ссылка на платежную систему: </b>
        %s

        %s <b>Инструкция:</b>
        1. Нажмите кнопку "Оплатить" ниже.
        2. Следуйте указаниям платежной системы.
        3. После оплаты вернитесь к боту и нажмите "Проверить оплату"
        """,
                TEXT_PAYMENT,
                payment.getAmount().setScale(2, RoundingMode.HALF_UP).doubleValue(),
                payment.getTransactionToken(),
                TEXT_WAIT,
                expiryTime,
                TEXT_LINK,
                plategaPayUrl + "?id=" + payment.getTransactionToken() + "&mh=" + merchantId,
                TEXT_DEVICE
        );
    }

    public String successText(Payment payment, User user) {
        // тк в payment храним в долларах
        if (payment.getMethod().equals(PaymentMethod.CRYPTO.getValue())) {
            CryptoPayment cryptoPayment = tonPaymentService.createUsdtPayment(payment);

            return String.format("""
            %s <b>Оплата подтверждена!</b>
            
            Ваш баланс пополнен на <b>%s ₽</b>
            Сумма в USDT: <b>%s ₽</b>
            Текущий баланс: <b>%.2f ₽</b>
            
            Спасибо за использование нашего сервиса!
            """,
                  TEXT_CHECK,
                  cryptoPayment.getAmountRub(),
                  cryptoPayment.getAmountUsdt(),
                  user.getBalance());
        } else if (payment.getMethod().equals(PaymentMethod.SBP.getValue())) {
            return String.format("""
            %s <b>Оплата подтверждена!</b>
            
            Ваш баланс пополнен на <b>%s ₽</b>
            Текущий баланс: <b>%.2f ₽</b>
            
            Спасибо за использование нашего сервиса!
            """,
                    TEXT_CHECK,
                    payment.getAmount(),
                    user.getBalance());
        }

        return "";
    }

    public String iosInstructionText () {
        return """
         %s <b>iOS — Happ</b>

         <i>Шаг 1:</i> Скачайте <b>Happ</b> из App Store
         %s <a href="https://apps.apple.com/ru/app/happ-proxy-utility-plus/id6746188973">Скачать Happ</a>

         <i>Шаг 2:</i> Откройте Telegram → Скопируйте <b>ссылку на подписку</b> в разделе <i>Подписка</i>

         <i>Шаг 3:</i> Откройте <b>Happ</b>: <b>Вставьте ссылку в: Из Буфера</b>

         <i>Шаг 4:</i> iOS запросит разрешение VPN → <b>"Разрешить"</b>

         <i>Шаг 5:</i> Выберите сервер и нажмите <b>"Подключиться"</b> на главной странице сверху

         %s <b>Важно:</b> Проверьте, что конфиг импортировался и сервера появились в подписке

         %s <b>Проблемы?</b>
         • Проверьте срок подписки и количество устройств в боте
         • Вставьте ссылку заново
         • Изучите ошибку или обратитесь в поддержку
         """.formatted(TEXT_LOCK, TEXT_LINK, TEXT_WARN, TEXT_BULB);
    }

    public String windowsInstructionText() {
        return """
         %s <b>Windows — Koala Clash</b>

         <i>Шаг 1:</i> Скачайте <b>Koala Clash</b> для Windows
         %s <a href="https://github.com/coolcoala/clash-verge-rev-lite/releases/download/v0.2.10/Koala.Clash_x64-setup.exe">Скачать Koala Clash</a>

         <i>Шаг 2:</i> Откройте Telegram → Скопируйте <b>ссылку на подписку</b>

         <i>Шаг 3:</i> Откройте <b>Koala Clash</b>: <b>Вставьте ссылку</b> в: Профили -> Нажмите на + и вставьте скопированную подписку

         <i>Шаг 4:</i> Нажмите <b>"Подключиться"</b>, менять сервера по кнопке: <b>Прокси</b> -> <b>NesVPN</b> и дальше выбираете сервер или на <b>Главная</b> под кнопкой подключения %s

         %s <b>Важно:</b> Проверьте, что конфиг импортировался в разделе <i>Прокси</i> и есть несколько серверов, пропингуйте их

         %s <b>Проблемы?</b>
         • Проверьте срок подписки и количество устройств в боте
         • Вставьте ссылку заново
         • Изучите ошибку или обратитесь в поддержку
         """.formatted(TEXT_DEVICE, TEXT_LINK, TEXT_CHECK, TEXT_WARN, TEXT_BULB);
    }

    public String macosInstructionText() {
        return """
         %s <b>MacOS — Koala Clash</b>

         <i>Шаг 1:</i> Скачайте <b>Koala Clash</b> для MacOS
         %s Ссылки для скачивания:
         • <a href="https://github.com/coolcoala/clash-verge-rev-lite/releases/download/v0.2.10/Koala.Clash_aarch64.dmg">Koala Clash apple silicon</a>
         • <a href="https://github.com/coolcoala/clash-verge-rev-lite/releases/download/v0.2.10/Koala.Clash_x64.dmg">Koala Clash intel</a>

         <i>Шаг 2:</i> Откройте Telegram → Скопируйте <b>ссылку на подписку</b>

         <i>Шаг 3:</i> Откройте <b>Koala Clash</b>: <b>Вставьте ссылку</b> в: Профили -> Нажмите на + и вставьте скопированную подписку

         <i>Шаг 4:</i> Нажмите <b>"Подключиться"</b>, менять сервера по кнопке: <b>Прокси</b> -> <b>NesVPN</b> и дальше выбираете сервер или на <b>Главная</b> под кнопкой подключения %s

         %s <b>Важно:</b> Проверьте, что конфиг импортировался в разделе <i>Прокси</i> и есть несколько серверов, пропингуйте их

         %s <b>Проблемы?</b>
         • Проверьте срок подписки и количество устройств в боте
         • Вставьте ссылку заново
         • Изучите ошибку или обратитесь в поддержку
         """.formatted(TEXT_DEVICE, TEXT_LINK, TEXT_CHECK, TEXT_WARN, TEXT_BULB);
    }

    public String androidInstructionText() {
        return """
         %s <b>Android — Happ</b>

         <i>Шаг 1:</i> Скачайте <b>Happ</b>
         %s <a href="https://play.google.com/store/apps/details?id=com.happproxy">Скачать Happ из Play Market</a>

         <i>Шаг 2:</i> Откройте Telegram → Скопируйте <b>ссылку на подписку</b>

         <i>Шаг 3:</i> Откройте <b>Happ</b> → Вставьте ссылку по кнопке снизу слева <i>Из буфера</i> или <i>+ в верхнем левом углу</i>

         <i>Шаг 4:</i> Нажмите <b>"Подключиться"</b> %s

         %s <b>Важно:</b> Проверьте, что конфиг импортировался в разделе <i>Профили</i> и есть несколько серверов

         %s <b>Проблемы?</b>
         • Проверьте срок подписки и количество устройств в боте
         • Вставьте ссылку заново
         • Изучите ошибку или обратитесь в поддержку
         """.formatted(TEXT_DEVICE, TEXT_LINK, TEXT_CHECK, TEXT_WARN, TEXT_BULB);
    }

    public String successSubscribeProvidedText() {
        return """
        %s <b>Подписка скоро обновится</b>
        
        %s Вы успешно оплатили подписку!
        
        Ожидайте, в течение пары секунд
        ваша ссылка обновится.
        """.formatted(TEXT_WAIT, TEXT_CHECK);
    }

    public String dataNotFoundText() {
        return  """
            %s <b>Данные не найдены</b>

            Вернитесь назад:
            """.formatted(TEXT_ERROR);
    }

    public String notEnoughMoneyMessage(Integer price, BigDecimal balance) {
        return String.format("""
            %s <b>Недостаточно средств</b>
            
            <b>Стоимость:</b> %d₽
            <b>Ваш баланс:</b> %.2f₽
            
            Пополните баланс для продления подписки.
            """, TEXT_ERROR, price, balance);
    }

    public String extendSubscribeConfirmText(String planName, Integer planPrice, String currentValidTo, String newValidTo, BigDecimal balance) {
        return  String.format("""
            %s <b>Подтверждение тарифа</b>
            
            %s <b>Тариф:</b> %s
            %s <b>Стоимость:</b> %d₽
            
            <b>Текущий срок действия:</b>
            %s
            
            <b>После продления:</b>
            %s
            
            <b>Ваш баланс:</b> %.2f₽
            
            Подтвердите продление подписки
            """,
                TEXT_CHECK,
                TEXT_PACKAGE,
                planName,
                TEXT_MONEY,
                planPrice,
                currentValidTo,
                newValidTo,
                balance
        );
    }

    public String tokenNotFoundText() {
        return """
            %s <b>Продление подписки</b>
            
            У вас нет активной подписки, вернитесь назад:
            """.formatted(TEXT_ERROR);
    }

    public String extendSubscriptionText(BigDecimal balance, String validTo, Long daysLeft) {
        return String.format("""
            %s <b>Продление подписки</b>
            
            %s <b>Ваш баланс:</b> %.2f₽
            
            <b>Текущий срок действия:</b>
            %s
            
            %s <b>Осталось дней:</b> %d
            
            %s <b>Выберите срок продления:</b>
            Чем дольше срок, тем выгоднее цена!
            """,
                TEXT_REFRESH,
                TEXT_MONEY,
                balance,
                validTo,
                TEXT_WAIT,
                daysLeft
                ,
                TEXT_USDT
        );
    }

    public String subscriptionExpirationNotificationText(SubscriptionExpirationNotificationType type, String validTo) {
        return switch (type) {
            case TWO_DAYS -> String.format("""
                %s <b>Скоро закончится подписка</b>

                <b>Срок действия:</b>
                %s

                %s До окончания осталось около <b>2 дней</b>.

                %s Продлите подписку заранее, чтобы VPN продолжил работать без перерыва.

                Нажмите кнопку ниже, чтобы открыть подписку.
                """, TEXT_WARN, validTo, TEXT_WAIT, TEXT_BULB);
            case ONE_DAY -> String.format("""
                %s <b>Подписка заканчивается завтра</b>

                <b>Срок действия:</b>
                %s

                %s Остался примерно <b>1 день</b>.

                %s Лучше продлить сейчас, чтобы доступ не прервался.

                Нажмите кнопку ниже, чтобы открыть подписку.
                """, TEXT_DATE, validTo, TEXT_WARN, TEXT_BULB);
            case EXPIRED -> String.format("""
                %s <b>Подписка закончилась</b>

                <b>Срок действия был до:</b>
                %s

                %s VPN-доступ мог остановиться.

                %s Продлите подписку, чтобы снова подключиться к сервису.

                Нажмите кнопку ниже, чтобы открыть подписку.
                """, TEXT_ERROR, validTo, TEXT_LOCK, TEXT_REFRESH);
        };
    }

    public String subscriptionExpirationAutoRenewalFailedText(
            SubscriptionExpirationNotificationType type,
            String validTo,
            Integer planPrice,
            BigDecimal balance
    ) {
        return subscriptionExpirationNotificationText(type, validTo).trim() + String.format("""


            %s <b>Автопродление не удалось.</b>

            Стоимость продления: <b>%d₽</b>
            Ваш баланс: <b>%.2f₽</b>

            Пополните баланс, чтобы подписка оставалась активной.
            """,
                TEXT_PAYMENT,
                planPrice,
                balance
        );
    }

    public String subscriptionAutoRenewalSuccessText(Integer planPrice, BigDecimal balanceAfter) {
        return String.format("""
            %s <b>Автопродление выполнено</b>

            У вас было включено автопродление, поэтому мы продлили подписку на <b>1 месяц</b>.

            %s <b>Списано:</b> %d₽
            %s <b>Баланс после списания:</b> %.2f₽

            %s Подписка скоро обновится, ссылка останется прежней.
            """,
                TEXT_CHECK,
                TEXT_PAYMENT,
                planPrice,
                TEXT_MONEY,
                balanceAfter,
                TEXT_REFRESH
        );
    }

    public String subscriptionText(
            Boolean isActive,
            String tokenUrl,
            String validTo,
            Long daysLeft,
            Integer devicesCount,
            boolean autoRenewalEnabled
    ) {
        String statusEmoji = Boolean.TRUE.equals(isActive) ? TEXT_CHECK : TEXT_ERROR;
        String statusText = Boolean.TRUE.equals(isActive) ? "Активна" : "Истекла";
        String devicesText = devicesCount != null
                ? String.format("%d / %d", devicesCount, Math.max(devicesCount, maxDevices))
                : "не удалось получить";
        String autoRenewalText = autoRenewalEnabled ? "включен" : "выключен";

        return String.format("""
            %s <b>Ваша подписка</b>
            
            %s <b>Статус:</b> %s
            
            %s <b>Ссылка для подключения:</b>
            <blockquote expandable><pre>%s</pre></blockquote>
            
            %s <b>Срок действия</b>
            Действует до: %s
            Осталось дней: %d
            
            %s <b>Устройств всего:</b> %s

            %s <b>Автопродление:</b> %s
            
            %s
            """,
                TEXT_DEVICE,
                statusEmoji,
                statusText,
                TEXT_LINK,
                tokenUrl,
                TEXT_DATE,
                validTo,
                daysLeft,
                TEXT_PEOPLE,
                devicesText,
                TEXT_AUTO_RENEWAL,
                autoRenewalText,
                daysLeft <= 7 && daysLeft > 0
                        ? "<i>" + TEXT_WARN + " Срок подписки истекает скоро! Продлите её.</i>"
                        : ""
        ).trim();
    }

    public String lucky777Text(boolean canSpin, String remainingText) {
        String status = canSpin
                ? TEXT_CHECK + " <b>Попытка доступна</b>"
                : TEXT_WAIT + " <b>Следующая попытка через:</b> " + remainingText;

        return String.format("""
            %s <b>Lucky 777</b>
            
            Раз в 12 часов можно прокрутить слот и попробовать выиграть дополнительные дни к подписке.
            
            %s <b>Что можно выиграть:</b>
            %s <b>777</b> — <b>+3 дня</b>
            %s <b>Любые три одинаковых символа</b> — <b>+1 день</b>
            
            <i>Бонус прибавится к текущему сроку подписки и не заменит оставшиеся дни.</i>
            
            %s
            
            Нажмите кнопку %s на нижней клавиатуре. Если Telegram отправляет обычный текст или кнопка не срабатывает, отправьте настоящий слот %s вручную через панель эмодзи.
            
            Бот засчитает только настоящий Telegram-слот, пока вы на этой странице.
            """, TEXT_LUCKY, TEXT_GIFT, TEXT_PARTY, TEXT_STAR, status, SLOT_DICE, SLOT_DICE);
    }

    public String lucky777ResultText(Integer diceValue, Integer rewardDays) {
        if (rewardDays == 3) {
            return String.format("""
                %s <b>777!</b>
                
                Вы выиграли <b>+3 дня</b> к подписке.
                Подписка скоро обновится.
                """, TEXT_PARTY);
        }

        if (rewardDays == 1) {
            return String.format("""
                %s <b>Три одинаковых символа!</b>
                
                Вы победили и здесь: <b>+1 день</b> к подписке.
                Подписка скоро обновится.
                """, TEXT_STAR);
        }

        return String.format("""
            %s <b>В этот раз без выигрыша</b>
            
            Следующая попытка будет доступна через 12 часов.
            """, TEXT_NO_WIN);
    }

    public String lucky777CooldownText(String remainingText) {
        return String.format("""
            %s <b>Попытка пока недоступна</b>
            
            Крутить барабан можно раз в 12 часов.
            
            Следующая попытка будет доступна через:
            <b>%s</b>
            """, TEXT_WAIT, remainingText);
    }

    public String lucky777AvailableNotificationText() {
        return String.format("""
            %s <b>Рулетка снова доступна</b>
            
            Прошло 12 часов, и вы снова можете испытать удачу в Lucky 777.
            
            Откройте раздел подписки, перейдите в Lucky 777 и отправьте настоящий слот %s. Бонусные дни, если выпадет выигрыш, прибавятся к текущему сроку подписки.
            """, TEXT_LUCKY, SLOT_DICE);
    }

    public String lucky777InvalidDiceText() {
        return String.format("""
            %s <b>Нужен настоящий слот</b>
            
            Отправьте именно настоящий Telegram-слот %s.
            Обычный текст или другой dice не засчитывается.
            """, TEXT_ALERT, SLOT_DICE);
    }

    public String lucky777KeyboardButtonText() {
        return String.format("""
            %s <b>Нужен настоящий слот</b>
            
            Если нижняя клавиатура отправила обычный текст или не сработала, отправьте слот %s вручную.
            
            Нужно выбрать настоящий Telegram-слот %s через панель эмодзи прямо в этом чате.
            """, TEXT_ALERT, SLOT_DICE, SLOT_DICE);
    }

    public String lucky777ForwardedDiceText() {
        return String.format("""
            %s <b>Пересланный слот не засчитывается</b>
            
            Отправьте новый слот %s прямо в этот чат.
            Так бот сможет честно проверить результат именно вашей попытки.
            """, TEXT_ALERT, SLOT_DICE);
    }

    public String lucky777NoTokenText() {
        return String.format("""
            %s <b>Lucky 777 недоступен</b>
            
            Сначала нужно создать подписку.
            После этого бонусный слот появится в разделе подписки.
            """, TEXT_ALERT);
    }

    public String channelSubscribeText() {
        return  """
            %s <b>Рекомендуем подписаться на канал</b>
            
            <i>%s Чтобы быть в курсе всех новостей и анонсов, просим подписаться</i>
            """.formatted(TEXT_DEVICE, TEXT_BULB);
    }

    public String awaitingBalanceCryptoText(Double rubRate) {
        String formattedRate = String.format("%.2f", rubRate);

        return  """
            %s <b>Пополнение баланса USDT (TON)</b>
        
            Введите сумму пополнения от <b>1$</b> до <b>25$</b>
        
            <b>Цена за 1 USDT:</b> %s руб.
        
            Например: <code>5</code>
            """.formatted(TEXT_MONEY, formattedRate);
    }

    public String awaitingBalanceRubText() {
        return  """
            %s <b>Пополнение баланса СБП</b>
        
            Введите сумму пополнения от <b>100₽</b> до <b>2000₽</b>
        
            Например: <code>500</code>
            """.formatted(TEXT_PAYMENT);
    }

    public String errorPlategaText() {
        return """
            %s <b>Не удалось обработать платёж</b>

            Сейчас возникла ошибка на стороне платёжного провайдера.
            Попробуйте выполнить оплату ещё раз или вернитесь немного позже.
            """.formatted(TEXT_NO_WIN);
    }

    public String inputErrorText(String title, String description) {
        return """
            %s <b>%s</b>
            
            %s
            """.formatted(TEXT_ERROR, title, description);
    }

    public String balanceHistoryText(List<BalanceTransaction> history) {
        StringBuilder historyText = new StringBuilder();

        for (BalanceTransaction tx : history) {
            String sign = tx.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            historyText.append(String.format(
                    "<b>%s %s%.2f₽</b>%n<i>%s</i>%n%s%n%n",
                    tx.getType().getDisplayName(),
                    sign,
                    tx.getAmount(),
                    tx.getDescription() != null ? tx.getDescription() : "Без описания",
                    tx.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            ));
        }

        String text = String.format("""
            <b>%s История операций</b>

            Последние 20 транзакций:
        
            %s
            """,
                TEXT_CHART,
                history.isEmpty() ? "<i>История пуста, вы не совершили ни одной транзакции</i>" : "<blockquote expandable>" + historyText + "</blockquote>"
        );

        if (text.length() > 4096) {
            text = text.substring(0, 4090) + "\n...";
        }

        return  text;
    }

    public String balanceText(BigDecimal balance) {
        return  String.format("""
            <b>%s Ваш баланс</b>

            <b>Текущий баланс:</b> %.2f₽

            %s <i>Баланс используется для:</i>
            • Оплаты VPN подписок
            • Продления активных подписок

            %s <i>Пополняйте баланс и получайте бонусы за покупки рефералов!</i>

            Выберите действие %s
            """,
                TEXT_MONEY,
                balance,
                TEXT_BULB,
                TEXT_GIFT,
                TEXT_POINT_DOWN
        );
    }

    public String instructionsText() {
        return  """
            %s <b>Инструкция по подключению NesVPN</b>

            <b>Общий процесс:</b>
            1. Скачай VPN-клиент: Happ, Koala Clash или FlClashX
            2. <i>Импорт подписки</i> из Telegram
            3. Проверь <i>количество ссылкок</i> в подписке
            4. <i>Подключиться</i> %s

            %s Иногда необходимо обновить подписку <b>2-3 раза</b> и <b>обязательно включить HWID</b>

            Выберите вашу платформу для более подробной инструкции %s
            """.formatted(TEXT_BOOK, TEXT_CHECK, TEXT_WARN, TEXT_POINT_DOWN);
    }

    public String referralsText(String referralLink, Integer referralSize, BigDecimal totalEarnings, String referralsList) {
        return String.format("""
            <b>%s Реферальная программа NesVPN</b>

            <b>%s Как это работает:</b>
            • Каждый друг по вашей ссылке = <b>%s%% от суммы</b> каждой покупки
            • Деньги с покупок рефералов <b>начисляются вам на баланс</b> автоматически
            • <b>Друг получит 50 рублей</b> на баланс
            • <b>Нет ограничений</b> по количеству приглашенных

            <b>%s Ваша ссылка:</b> %s

            <b>%s Статистика:</b>
            Рефералов: %d
            Ваш доход: %.2f₽

            <b>%s Ваши рефералы:</b>
            <blockquote expandable>%s</blockquote>
            """,
                TEXT_PEOPLE,
                TEXT_USDT,
                referralPercent,
                TEXT_LINK,
                referralLink,
                TEXT_CHART,
                referralSize,
                totalEarnings,
                TEXT_NOTE,
                referralSize == 0 ? "Пока нет рефералов " + TEXT_NO_WIN : referralsList
        );
    }

    public String profileText(String displayName, BigDecimal balance, Integer referralCount, String createdAt,String referralLink) {
        return  String.format("""
        %s <b>Ваш профиль</b>
        
        %s <b>Данные аккаунта</b>
        Имя: %s
        Регистрация: %s
        
        %s <b>Финансы и рефералы</b>
        Баланс: %.2f₽
        Рефералов: %d
        
        %s <b>Ваша реферальная ссылка:</b>
        <i>%s</i>
        """,
                TEXT_PROFILE,
                TEXT_NAME,
                displayName,
                createdAt,
                TEXT_MONEY,
                balance,
                referralCount,
                TEXT_LINK,
                referralLink
        );
    }

    public String hwidDevicesText(List<HwidDevice> hwidDevices) {
        int count = hwidDevices != null ? hwidDevices.size() : 0;
        int maxUserDevices = Math.max(maxDevices, count);

        String infoText;

        if (count == 0) {
            infoText = """
            <i>Вы пока ещё не подключили ни одного устройства.
            После подключения VPN ваше устройство автоматически появится в этом списке.</i>
            """;
        } else if (count == maxUserDevices) {
            infoText = """
            <b>Достигнут лимит устройств.</b>
    
            <b>Если вы хотите использовать VPN на другом устройстве, удалите одно из текущих.</b>
            """;
        } else {
            infoText = """
            <i>Вы можете подключить ещё устройства.
            Если потребуется освободить место, удалите одно из текущих.</i>
            """;
        }

        return """
            %s <b>Управление устройствами</b>

            %s <b>Сейчас привязано устройств:</b> %d из %d

            %s
            <b>После удаления устройства обновите подписку в VPN-клиенте.</b>
            Это нужно сделать на <b>всех оставшихся устройствах</b>, чтобы они получили обновлённые настройки подключения.
            
            %s <i>Выберите устройство, которое будет удалено</i>
            """.formatted(TEXT_LOCK, TEXT_DEVICE, count, maxUserDevices, infoText, TEXT_POINT_DOWN);
    }

    public String hwidDeviceDeleteConfirm() {
        return """
            %s <b>Удаление устройства</b>
            
            Вы уверены, что хотите удалить это устройство?
            
            <b>После удаления устройства обновите подписку в VPN-клиенте на всех оставшихся устройствах, удаленное устройство будет отключено до повторного добавления.</b>
            """.formatted(TEXT_WARN);
    }

    public String hwidDeviceDeleteSuccess() {
        return """
            %s <b>Устройство успешно удалено</b>

            %s <b>Обновите подписку в VPN-клиенте на всех оставшихся устройствах и переподключитесь к ней</b>
            
            <i>Изменения могут отобразиться не сразу. Пожалуйста, подождите некоторое время.</i>
            """.formatted(TEXT_CHECK, TEXT_WARN);
    }

    public String hwidDeviceDeleteError() {
        return """
            %s <b>Не удалось удалить устройство</b>

            <i>Произошла ошибка при удалении устройства. Пожалуйста, попробуйте снова немного позже.</i>
            """.formatted(TEXT_ERROR);
    }

    public String hwidDevicesUnavailableText() {
        return """
            %s <b>Не удалось получить список устройств</b>

            <i>Попробуйте открыть этот раздел немного позже.</i>
            """.formatted(TEXT_ERROR);
    }

    public String startText(String displayName) {
        return String.format("""
            %s Добро пожаловать в <b>NesVPN</b>, <b>%s</b>
            
            %s <b>Быстрый, безопасный и стабильный VPN для повседневного использования</b>
            
            %s <b>Преимущества:</b>
            <i>• Высокая скорость соединения</i>
            <i>• Работает в исключительных ситуациях</i>
            <i>• Низкая цена</i>
            <i>• Поддержка всех устройств</i>
            <i>• Бесплатный тестовый период</i>

            <b>Выберите действие в меню ниже</b> %s
            """, TEXT_WAVE, displayName != null ? displayName : "Дорогой друг", TEXT_LOCK, TEXT_STAR, TEXT_POINT_DOWN);
    }

    public String errorCreatePaymentText(Integer pendingCount) {
        return String.format("""
            %s <b>Невозможно создать новый платёж</b>
            
            У вас уже <b>%d из 5</b> возможных активных платежей.
            
            %s <b>Что делать?</b>
            • Оплатите один из существующих платежей
            • Дождитесь истечения срока
            
            %s Нажмите кнопку для просмотра платежей
            """, TEXT_WARN, pendingCount, TEXT_ALERT, TEXT_POINT_DOWN);
    }

    public String expiredPaymentText(String transactionId) {
        return """
            %s <b>Срок платежа истек: %s</b>
        
            Платеж больше не действителен.
            Создайте новый платеж для пополнения.
            """.formatted(TEXT_WAIT, transactionId);

    }

    public String checkPaymentNotFoundText(String currentTime) {
        return String.format("""
      
            %s <b>Проверка в %s</b>
    
            %s <b>Платёж ещё не найден</b>
    
            Платеж еще не поступил или обрабатывается.
            Пожалуйста, попробуйте снова через 10 секунд.
            """, TEXT_WAIT, currentTime, TEXT_ERROR);
    }

    public String checkPaymentCooldownText(String currentTime, Long remaining) {
        return String.format("""
            %s <b>Проверка в %s</b>
    
            <b>Слишком частые проверки</b>
    
            Проверять платеж можно раз в 10 секунд.
            Подождите ещё <b>%d секунд</b>.
            """, TEXT_WAIT, currentTime, remaining);
    }

    public String checkPaymentErrorText(String transactionId) {
        return  String.format("""
            %s <b>Платеж не найден</b>
    
            ID транзакции: <code>%s</code>
    
            Пожалуйста, проверьте данные или создайте новый платеж.
            """, TEXT_ERROR, transactionId);
    }

    public String aboutServiceText() {
        return """
            <b>Юридическая информация</b>
        
            Используя наш сервис, вы подтверждаете, что ознакомились и соглашаетесь со следующими документами:
        
            • <a href="https://telegra.ph/Politika-konfidencialnosti-04-01-26" target="_blank">Политика конфиденциальности</a>
        
            • <a href="https://telegra.ph/Polzovatelskoe-soglashenie-04-01-19" target="_blank">Пользовательское соглашение</a>
        
            Продолжая пользоваться ботом и сервисом, вы принимаете условия указанных документов.
            """;
    }

    public String topUpText() {
        return """
        %s <b>Пополнение баланса</b>
    
        Вы хотите пополнить баланс.
        Для этого выберите метод пополнения, а далее укажите сумму.
    
        Выберите способ оплаты:
        """.formatted(TEXT_MONEY);
    }

    public String newReferralText(String displayName, Long id) {
        return  String.format("""
            %s <b>По вашей реферальной ссылке зарегистрировался новый пользователь!</b>
            
            %s С его покупок вы будете получать <b>%s%%</b> на баланс
            
            %s <b>Пользователь:</b>
            <i>%s</i>
            %s <b>ID:</b> <code>%d</code>
            """,
                TEXT_PARTY,
                TEXT_MONEY,
                referralPercent,
                TEXT_PROFILE,
                displayName != null ? displayName : "Новый пользователь",
                TEXT_NOTE,
                id);
    }

    public String broadcastAwaitingPostText() {
        return """
            %s <b>Рассылка</b>

            Отправьте следующим сообщением пост, который нужно разослать пользователям бота.

            Можно отправить обычный текст или пост с изображением и подписью.
            После отправки бот запустит рассылку и пришлёт статистику по завершении.
            """.formatted(TEXT_NOTE);
    }

    public String broadcastStartedText(Integer totalRecipients) {
        return """
            %s <b>Рассылка запущена</b>

            Получателей в очереди: <b>%d</b>

            Когда отправка завершится, бот пришлёт итоговую статистику администраторам.
            """.formatted(TEXT_CHECK, totalRecipients != null ? totalRecipients : 0);
    }

    public String broadcastCreatedText(Long campaignId, Integer totalRecipients) {
        String recipientsText = totalRecipients != null && totalRecipients > 0
                ? "<b>%d</b>".formatted(totalRecipients)
                : "<b>подготавливаются</b>";

        return """
            %s <b>Рассылка поставлена в очередь</b>

            Кампания: <code>%d</code>
            Источник: <b>ручной</b>
            Получателей: %s

            Бот начнёт отправку автоматически и пришлёт итоговую статистику после завершения.
            """.formatted(
                TEXT_NOTE,
                campaignId,
                recipientsText
        );
    }

    public String broadcastProgressText(BroadcastProgress progress) {
        String statusText = broadcastStatusText(progress.status());
        int totalRecipients = progress.totalRecipients() != null ? progress.totalRecipients() : 0;
        long sentCount = progress.sentCount() != null ? progress.sentCount() : 0L;
        long failedCount = progress.failedCount() != null ? progress.failedCount() : 0L;
        long pendingCount = progress.pendingCount() != null ? progress.pendingCount() : 0L;
        long attemptedCount = sentCount + failedCount;
        String recipientsText = totalRecipients > 0 ? "<b>%d</b>".formatted(totalRecipients) : "<b>подготавливаются</b>";
        String updatedAt = Formatter.formatMoscow(LocalDateTime.now(), "HH:mm:ss");

        return """
            %s <b>Рассылка поставлена в очередь</b>

            Кампания: <code>%d</code>
            Источник: <b>ручной</b>
            Статус: <b>%s</b>

            Получателей: %s
            Попытались отправить: <b>%d</b>
            Успешно: <b>%d</b>
            С ошибкой: <b>%d</b>
            Осталось в очереди: <b>%d</b>

            Обновлено: <code>%s</code>
            """.formatted(
                TEXT_NOTE,
                progress.campaignId(),
                statusText,
                recipientsText,
                attemptedCount,
                sentCount,
                failedCount,
                pendingCount,
                updatedAt
        );
    }

    public String broadcastAlreadyRunningText(Long campaignId) {
        String campaignText = campaignId != null ? "<code>%d</code>".formatted(campaignId) : "<code>неизвестно</code>";

        return """
            %s <b>Рассылка не запущена</b>

            Уже идёт другая рассылка: %s.
            Новый пост не поставлен в очередь, чтобы пользователи не получили несколько рассылок одновременно.
            """.formatted(TEXT_WAIT, campaignText);
    }

    public String broadcastStatsText(BroadcastStats stats) {
        return """
            %s <b>Рассылка завершена</b>

            Кампания: <code>%d</code>
            Источник: <b>ручной</b>

            Всего пользователей: <b>%d</b>
            Успешно отправлено: <b>%d</b>
            С ошибкой: <b>%d</b>
            """.formatted(
                TEXT_CHART,
                stats.campaignId(),
                stats.totalRecipients() != null ? stats.totalRecipients() : 0,
                stats.sentCount(),
                stats.failedCount()
        );
    }

    private String broadcastStatusText(BroadcastCampaignStatus status) {
        return switch (status) {
            case PREPARING -> "подготовка получателей";
            case PROCESSING -> "идёт отправка";
            case COMPLETED -> "завершена";
            case FAILED -> "ошибка";
        };
    }
}
