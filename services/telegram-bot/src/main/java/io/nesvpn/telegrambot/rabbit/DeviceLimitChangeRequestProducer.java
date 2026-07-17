package io.nesvpn.telegrambot.rabbit;

import io.nesvpn.rabbitmqconfig.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceLimitChangeRequestProducer {

    private final RabbitTemplate rabbitTemplate;

    @Retryable(
            retryFor = Exception.class,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendDeviceLimitChangeTask(DeviceLimitChangeRequest request) {
        log.info("Задача на изменение лимита устройств отправлена: userId={}, orderId={}",
                request.getUserId(), request.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY_DEVICE_LIMIT_CHANGE_REQUEST,
                request
        );
    }

    @Recover
    public void fallback(Exception e, DeviceLimitChangeRequest request) {
        log.error("Запрос на изменение лимита устройств не отправился, order {}", request.getOrderId());
    }
}
