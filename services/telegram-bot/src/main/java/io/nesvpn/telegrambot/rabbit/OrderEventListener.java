package io.nesvpn.telegrambot.rabbit;

import io.nesvpn.telegrambot.handler.VpnBot;
import io.nesvpn.telegrambot.util.DisplayTelegramUsername;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final LinkRequestProducer linkRequestProducer;
    private final VpnBot vpnBot;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Транзакция заказа {} успешно завершена. Отправляем в RabbitMQ...", event.orderId());
        String username = DisplayTelegramUsername.getDisplayName(vpnBot, event.tgId());
        linkRequestProducer.sendLinkGenerationTask(
                LinkRequest.builder()
                        .userId(event.userId())
                        .orderId(event.orderId())
                        .planId(event.planId())
                        .tgUsername(username)
                        .build()
        );
    }
}