package io.nesvpn.subscribelinkservice.rabbit;

import io.nesvpn.rabbitmqconfig.RabbitConfig;
import io.nesvpn.subscribelinkservice.service.LinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class LinkRequestListener {

    private final LinkService linkService;
    private final RabbitTemplate rabbitTemplate; // Нужен для отправки на кладбище

    private static final int MAX_RETRIES = 5;

    @RabbitListener(queues = RabbitConfig.ROUTING_KEY_REQUEST, concurrency = "10-50")
    public void onLinkRequest(
            @Payload LinkRequest request,
            @Header(name = "x-death", required = false) List<Map<String, Object>> xDeath) {
        String requestId = request.getUserId() + "-" + request.getOrderId();
        long retryCount = getRetryCount(xDeath);
        if (retryCount >= MAX_RETRIES) {
            log.error("ЗАКАЗ {} ИСЧЕРПАЛ ЛИМИТ ПОПЫТОК ({}). Отправляем на кладбище!", requestId, retryCount);
            rabbitTemplate.convertAndSend(RabbitConfig.DLX_EXCHANGE, RabbitConfig.ROUTING_KEY_FAILED, request);
            return;
        }
        log.info("[Link listener] Генерация ссылки: {} (Попытка {})", requestId, retryCount + 1);
        try {
            linkService.process(request.getUserId(), request.getPlanId(), request.getOrderId())
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofMinutes(1));
            log.info("[Link listener] Заказ {} успешно закрыт!", requestId);
        } catch (Exception e) {
            log.warn("[Link listener] Ошибка генерации {}. Отправляю в DLQ на 30 секунд. Причина: {}", requestId, e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Отправляем на круг через DLQ", e);
        }
    }

    private long getRetryCount(List<Map<String, Object>> xDeath) {
        if (xDeath != null && !xDeath.isEmpty()) {
            Map<String, Object> deathProps = xDeath.getFirst();
            Long count = (Long) deathProps.get("count");
            return count != null ? count : 0;
        }
        return 0;
    }
}