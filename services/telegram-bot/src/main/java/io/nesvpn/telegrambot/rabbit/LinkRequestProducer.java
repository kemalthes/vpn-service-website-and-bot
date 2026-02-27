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
public class LinkRequestProducer {

    private final RabbitTemplate rabbitTemplate;

    @Retryable(
            retryFor = Exception.class,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendLinkGenerationTask(LinkRequest request) {
        log.info("Задача на генерацию отправлена: userId={}, orderId={}",
                request.getUserId(), request.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY_REQUEST,
                request
        );
    }

    @Recover
    public void fallback(Exception e, LinkRequest request) {
        log.error("Запрос не отправился, order {}", request.getOrderId());
    }
}