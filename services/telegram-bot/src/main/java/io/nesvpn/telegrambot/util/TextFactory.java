package io.nesvpn.telegrambot.util;

import io.nesvpn.telegrambot.dto.CryptoPayment;
import io.nesvpn.telegrambot.dto.HwidDevice;
import io.nesvpn.telegrambot.enums.PaymentMethod;
import io.nesvpn.telegrambot.model.BalanceTransaction;
import io.nesvpn.telegrambot.model.Payment;
import io.nesvpn.telegrambot.model.User;
import io.nesvpn.telegrambot.services.TonPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TextFactory {
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

    public String iosInstructionText () {
        return """
         🔐 *iOS — Happ*

         _Шаг 1:_ Скачайте *Happ* из App Store
         🔗 [Скачать Happ](https://apps.apple.com/ru/app/happ-proxy-utility-plus/id6746188973)

         _Шаг 2:_ Откройте Telegram → Скопируйте *ссылку на подписку* в разделе _Подписка_

         _Шаг 3:_ Откройте *Happ*: *Вставьте ссылку в: Из Буфера*

         _Шаг 4:_ iOS запросит разрешение VPN → *"Разрешить"*

         _Шаг 5:_ Выберите сервер и нажмите *"Подключиться"* на главной странице сверху

         ⚠️ *Важно:* Проверьте, что конфиг импортировался и сервера появились в подписке

         💡 *Проблемы?*
         • Проверьте срок подписки и количество устройств в боте
         • Вставьте ссылку заново
         • Изучите ошибку или обратитесь в поддержку
         """;
    }

    public String windowsInstructionText() {
        return """
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
    }

    public String macosInstructionText() {
        return """
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
    }

    public String androidInstructionText() {
        return """
         📱 *Android — Happ*

         _Шаг 1:_ Скачайте *Happ*
         🔗 [Скачать Happ из Play Market](https://play.google.com/store/apps/details?id=com.happproxy)

         _Шаг 2:_ Откройте Telegram → Скопируйте *ссылку на подписку*

         _Шаг 3:_ Откройте *Happ* → Вставьте ссылку по кнопке снизу слева _Из буфера_ или _+ в верхнем левом углу_

         _Шаг 4:_ Нажмите *"Подключиться"* ✅

         ⚠️ *Важно:* Проверьте, что конфиг импортировался в разделе _Профили_ и есть несколько серверов

         💡 *Проблемы?*
         • Проверьте срок подписки и количество устройств в боте
         • Вставьте ссылку заново
         • Изучите ошибку или обратитесь в поддержку
         """;
    }

    public String successSubscribeProvidedText() {
        return """
        ⏳ <b>Подписка скоро обновится</b>
        
        ✅ Вы успешно оплатили подписку!
        
        Ожидайте в течение пару минут,
        ваша ссылка обновится.
        """;
    }

    public String dataNotFoundText() {
        return  """
            ❌ <b>Данные не найдены</b>

            Вернитесь назад:
            """;
    }

    public String notEnoughMoneyMessage(Integer price, BigDecimal balance) {
        return String.format("""
            ❌ <b>Недостаточно средств</b>
            
            <b>Стоимость:</b> %d₽
            <b>Ваш баланс:</b> %.2f₽
            
            Пополните баланс для продления подписки.
            """, price, balance);
    }

    public String extendSubscribeConfirmText(String planName, Integer planPrice, String currentValidTo, String newValidTo, BigDecimal balance) {
        return  String.format("""
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
                planName,
                planPrice,
                currentValidTo,
                newValidTo,
                balance
        );
    }

    public String tokenNotFoundText() {
        return """
            ❌ <b>Продление подписки</b>
            
            У вас нет активной подписки, вернитесь назад:
            """;
    }

    public String extendSubscriptionText(BigDecimal balance, String validTo, Long daysLeft) {
        return String.format("""
            🔄 <b>Продление подписки</b>
            
            💳 <b>Ваш баланс:</b> %.2f₽
            
            📅 <b>Текущий срок действия:</b>
            %s
            
            ⏳ <b>Осталось дней:</b> %d
            
            💎 <b>Выберите срок продления:</b>
            Чем дольше срок, тем выгоднее цена!
            """,
                balance,
                validTo,
                daysLeft
        );
    }

    public String subscribeExpiringText(String validTo) {
        return String.format("""
        ⚠️ <b>Скоро закончится подписка</b>

        📅 <b>Срок действия вашей подписки истекает:</b>
        %s

        ⏳ До окончания осталось меньше 2 дней.

        💡 Чтобы сохранить доступ к сервису без перерыва, рекомендуем продлить подписку заранее.

        Нажмите кнопку ниже, чтобы продлить.
        """, validTo);
    }

    public String subscriptionText(Boolean isActive, String tokenUrl, String validTo, Long daysLeft, Integer devicesCount) {
        String statusEmoji = isActive ? "✅" : "❌";
        String statusText = isActive ? "Активна" : "Истекла";

        return String.format("""
            📱 <b>Ваша подписка</b>
            
            %s <b>Статус:</b> %s
            
            🔗 <b>Ссылка для подключения:</b>
            <blockquote expandable><pre>%s</pre></blockquote>
            
            📅 <b>Действует до:</b> %s
            ⏳ <b>Осталось дней:</b> %d
            
            👥 <b>Устройств всего:</b> %d / %d
            
            %s
            """,
                statusEmoji,
                statusText,
                tokenUrl,
                validTo,
                daysLeft,
                devicesCount,
                Math.max(devicesCount, maxDevices),
                daysLeft <= 7 && daysLeft > 0
                        ? "<i>⚠️ Срок подписки истекает скоро! Продлите её.</i>"
                        : ""
        ).trim();
    }

    public String channelSubscribeText() {
        return  """
            📱 <b>Рекомендуем подписаться на канал</b>
            
            <i>💡 Чтобы быть в курсе всех новостей и анонсов, просим подписаться</i>
            """;
    }

    public String awaitingBalanceCryptoText(Double rubRate) {
        String formattedRate = String.format("%.2f", rubRate);

        return  """
            💰 *Пополнение баланса USDT (TON)*
        
            Введите сумму пополнения от *1$* до *25$*
        
            *Цена за 1 USDT:* %s руб.
        
            Например: `5`
            """.formatted(formattedRate);
    }

    public String awaitingBalanceRubText() {
        return  """
            💰 *Пополнение баланса СБП*
        
            Введите сумму пополнения от *100₽* до *2000₽*
        
            Например: `500`
            """;
    }

    public String balanceHistoryText(List<BalanceTransaction> history) {
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
                history.isEmpty() ? "<i>История пуста, вы не совершили ни одной транзакции</i>" : "<blockquote expandable>" + historyText + "</blockquote>"
        );

        if (text.length() > 4096) {
            text = text.substring(0, 4090) + "\n...";
        }

        return  text;
    }

    public String balanceText(BigDecimal balance) {
        return  String.format("""
            *💰 Ваш баланс*

            💵 *Текущий баланс:* %.2f₽

            _Баланс используется для:_
            • Оплаты VPN подписок
            • Продления активных подписок

            💡 _Пополняйте баланс и получайте бонусы за покупки рефералов!_

            Выберите действие 👇
            """,
                balance
        );
    }

    public String instructionsText() {
        return  """
            📖 *Инструкция по подключению NesVPN*

            *Общий процесс:*
            1. Скачай VPN-клиент: Happ, Koala Clash или FlClashX
            2. _Импорт подписки_ из Telegram
            3. Проверь _количество ссылкок_ в подписке
            4. _Подключиться_ ✅

            ⚠️ Иногда необходимо обновить подписку *2-3 раза* и *обязательно включить HWID*

            Выберите вашу платформу для более подробной инструкции 👇
            """;
    }

    public String referralsText(String referralLink, Integer referralSize, BigDecimal totalEarnings, String referralsList) {
        return String.format("""
            <b>👥 Реферальная программа NesVPN</b>

            <b>💎 Как это работает:</b>
            • Каждый друг по вашей ссылке = <b>%s%% от суммы</b> каждой покупки
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
                referralPercent,
                referralLink,
                referralSize,
                totalEarnings,
                referralSize == 0 ? "Пока нет рефералов 😔" : referralsList
        );
    }

    public String profileText(String displayName, BigDecimal balance, Integer referralCount, String createdAt,String referralLink) {
        return  String.format("""
        👤 <b>Ваш профиль</b>
        
        📛 <i>Имя:</i> %s
        💰 <i>Баланс:</i> %.2f₽
        👥 <i>Рефералов:</i> %d
        📅 <i>Регистрация:</i> %s
        
        🔗 <b>Ваша реферальная ссылка:</b>
        <i>%s</i>
        """,
                displayName,
                balance,
                referralCount,
                createdAt,
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
            ⚠️ <b>Достигнут лимит устройств.</b>
    
            <b>Если вы хотите использовать VPN на другом устройстве, удалите одно из текущих.</b>
            """;
        } else {
            infoText = """
            <i>Вы можете подключить ещё устройства.
            Если потребуется освободить место, удалите одно из текущих.</i>
            """;
        }

        return """
            🔐 <b>Управление устройствами</b>

            📱 <b>Сейчас привязано устройств:</b> %d из %d

            %s
            ‼️ <b>После удаления устройства обновите подписку в VPN-клиенте.</b>
            Это нужно сделать на <b>всех оставшихся устройствах</b>, чтобы они получили обновлённые настройки подключения.
            
            👇 <i>Выберите устройство, которое будет удалено</i>
            """.formatted(count, maxUserDevices, infoText);
    }

    public String hwidDeviceDeleteConfirm() {
        return """
            ⚠️ <b>Удаление устройства</b>
            
            Вы уверены, что хотите удалить это устройство?
            
            ‼️ <b>После удаления устройства обновите подписку в VPN-клиенте на всех оставшихся устройствах, удаленное устройство будет отключено до повторного добавления.</b>
            """;
    }

    public String hwidDeviceDeleteSuccess() {
        return """
            ✅ <b>Устройство успешно удалено</b>

            ‼️ <b>Обновите подписку в VPN-клиенте на всех оставшихся устройствах и переподключитесь к ней</b>
            
            <i>Изменения могут отобразиться не сразу. Пожалуйста, подождите некоторое время.</i>
            """;
    }

    public String hwidDeviceDeleteError() {
        return """
            ❌ <b>Не удалось удалить устройство</b>

            <i>Произошла ошибка при удалении устройства. Пожалуйста, попробуйте снова немного позже.</i>
            """;
    }

    public String startText(String displayName) {
        return String.format("""
            👋 Добро пожаловать в <b>NesVPN</b>, <b>%s</b>
            
            🔐 <b>Быстрый, безопасный и стабильный VPN для повседневного использования</b>
            
            ⚡️ <b>Преимущества:</b>
            <i>• Высокая скорость соединения</i>
            <i>• Работает в исключительных ситуациях</i>
            <i>• Низкая цена</i>
            <i>• Поддержка всех устройств</i>
            <i>• Бесплатный тестовый период</i>

            <b>Выберите действие в меню ниже</b> 👇
            """, displayName != null ? displayName : "Дорогой друг");
    }

    public String errorCreatePaymentText(Integer pendingCount) {
        return String.format("""
            ⚠️ <b>Невозможно создать новый платёж</b>
            
            У вас уже <b>%d из 5</b> возможных активных платежей.
            
            🔴 <b>Что делать?</b>
            • Оплатите один из существующих платежей
            • Дождитесь истечения срока
            
            👇 Нажмите кнопку для просмотра платежей
            """, pendingCount);
    }

    public String expiredPaymentText(String transactionId) {
        return """
            ⌛️ <b>Срок платежа истек: %s</b>
        
            Платеж больше не действителен.
            Создайте новый платеж для пополнения.
            """.formatted(transactionId);

    }

    public String checkPaymentNotFoundText(String currentTime) {
        return String.format("""
      
            ⏰ <b>Проверка в %s</b>
    
            ❌ <b>Платёж ещё не найден</b>
    
            Платеж еще не поступил или обрабатывается.
            Пожалуйста, попробуйте снова через 10 секунд.
            """, currentTime);
    }

    public String checkPaymentCooldownText(String currentTime, Long remaining) {
        return String.format("""
            ⏰ <b>Проверка в %s</b>
    
            ⏳ <b>Слишком частые проверки</b>
    
            Проверять платеж можно раз в 10 секунд.
            Подождите ещё <b>%d секунд</b>.
            """, currentTime, remaining);
    }

    public String checkPaymentErrorText(String transactionId) {
        return  String.format("""
            ❌ <b>Платеж не найден</b>
    
            ID транзакции: <code>%s</code>
    
            Пожалуйста, проверьте данные или создайте новый платеж.
            """, transactionId);
    }

    public String aboutServiceText() {
        return """
            <b>Юридическая информация</b>
        
            Используя наш сервис, вы подтверждаете, что ознакомились и соглашаетесь со следующими документами:
        
            • <a href="https://telegra.ph/Politika-konfidencialnosti-08-15-17" target="_blank">Политика конфиденциальности</a>
        
            • <a href="https://telegra.ph/Polzovatelskoe-soglashenie-08-15-10" target="_blank">Пользовательское соглашение</a>
        
            Продолжая пользоваться ботом и сервисом, вы принимаете условия указанных документов.
            """;
    }

    public String topUpText() {
        return """
        💰 *Пополнение баланса*
    
        Вы хотите пополнить баланс.
        Для этого выберите метод пополнения, а далее укажите сумму.
    
        Выберите способ оплаты:
        """;
    }

    public String newReferralText(String displayName, Long id) {
        return  String.format("""
            🎉 <b>По вашей реферальной ссылке зарегистрировался новый пользователь!</b>
            
            💰 С его покупок вы будете получать <b>%s%%</b> на баланс
            
            👤 <b>Пользователь:</b>
            <i>%s</i>
            🆔 <b>ID:</b> <code>%d</code>
            """,
                referralPercent,
                displayName != null ? displayName : "Новый пользователь",
                id);
    }
}
