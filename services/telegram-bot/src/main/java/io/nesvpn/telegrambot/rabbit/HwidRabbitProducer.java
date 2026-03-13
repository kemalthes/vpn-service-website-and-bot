package io.nesvpn.telegrambot.rabbit;

import io.nesvpn.rabbitmqconfig.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HwidRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    public HwidResponse sendHwidRequest(HwidRequest request) {
        HwidResponse response = rabbitTemplate.convertSendAndReceiveAsType(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY_HWID_REQUEST,
                request,
                new ParameterizedTypeReference<>() {}
        );
        if (response != null) {
            return response;
        }
        log.warn("Empty HWID response");
        return HwidResponse.builder()
                .userId(request.getUserId())
                .method(request.getMethod())
                .success(false)
                .message("Empty response from hwid service")
                .build();
    }
}
