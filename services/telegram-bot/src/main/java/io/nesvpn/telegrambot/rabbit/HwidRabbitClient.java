package io.nesvpn.telegrambot.rabbit;

import io.nesvpn.rabbitmqconfig.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HwidRabbitClient {

    private final RabbitTemplate rabbitTemplate;

    public HwidResponse request(HwidRequest request) {
        Object response = rabbitTemplate.convertSendAndReceive(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY_HWID_REQUEST,
                request
        );
        if (response instanceof HwidResponse hwidResponse) {
            return hwidResponse;
        }
        log.warn("Unexpected HWID response type: {}", response == null ? "null" : response.getClass().getName());
        return HwidResponse.builder()
                .userId(request.getUserId())
                .method(request.getMethod())
                .success(false)
                .message("Empty response from hwid service")
                .build();
    }
}
