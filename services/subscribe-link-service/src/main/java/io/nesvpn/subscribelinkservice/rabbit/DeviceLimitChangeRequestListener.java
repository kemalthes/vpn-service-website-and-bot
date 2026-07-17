package io.nesvpn.subscribelinkservice.rabbit;

import io.nesvpn.rabbitmqconfig.RabbitConfig;
import io.nesvpn.subscribelinkservice.service.DeviceLimitChangeService;
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
public class DeviceLimitChangeRequestListener {

    private static final int MAX_RETRIES = 5;

    private final DeviceLimitChangeService deviceLimitChangeService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitConfig.ROUTING_KEY_DEVICE_LIMIT_CHANGE_REQUEST, concurrency = "3-10")
    public void onDeviceLimitChangeRequest(
            @Payload DeviceLimitChangeRequest request,
            @Header(name = "x-death", required = false) List<Map<String, Object>> xDeath) {
        String requestId = request.getUserId() + "-" + request.getOrderId();
        long retryCount = getRetryCount(xDeath);
        if (retryCount >= MAX_RETRIES) {
            log.error("[DeviceLimit listener] Заказ {} исчерпал лимит попыток ({}). Отправляем в failed.", requestId, retryCount);
            rabbitTemplate.convertAndSend(RabbitConfig.DLX_EXCHANGE, RabbitConfig.ROUTING_KEY_DEVICE_LIMIT_CHANGE_FAILED, request);
            return;
        }

        try {
            deviceLimitChangeService.process(request.getOrderId(), request.getTgUsername())
                    .subscribeOn(Schedulers.boundedElastic())
                    .block(Duration.ofMinutes(1));
            log.info("[DeviceLimit listener] Заказ {} успешно обработан", requestId);
        } catch (Exception e) {
            log.warn("[DeviceLimit listener] Ошибка изменения лимита {}. Отправляю в DLQ. Причина: {}", requestId, e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Отправляем изменение лимита на повтор через DLQ", e);
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
