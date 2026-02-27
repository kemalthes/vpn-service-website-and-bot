package io.nesvpn.telegrambot.rabbit;

import io.nesvpn.telegrambot.services.OrderService;
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderService.OrderPaidEvent event) {
        log.info("Транзакция заказа {} успешно завершена. Отправляем в RabbitMQ...", event.orderId());
        linkRequestProducer.sendLinkGenerationTask(
                LinkRequest.builder()
                        .userId(event.userId())
                        .orderId(event.orderId())
                        .planId(event.planId())
                        .build()
        );
    }
}