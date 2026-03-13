package io.nesvpn.subscribelinkservice.rabbit;

import io.nesvpn.rabbitmqconfig.RabbitConfig;
import io.nesvpn.subscribelinkservice.service.HwidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HwidRequestListener {

    private final HwidService hwidService;

    @RabbitListener(queues = RabbitConfig.ROUTING_KEY_HWID_REQUEST, concurrency = "3-10")
    public HwidResponse onHwidRequest(@Payload HwidRequest request) {
        try {
            return hwidService.process(request);
        } catch (Exception ex) {
            log.error("[Hwid listener] Unhandled error: {}", ex.getMessage(), ex);
            return HwidResponse.builder()
                    .userId(request.getUserId())
                    .method(request.getMethod())
                    .success(false)
                    .message(ex.getMessage())
                    .build();
        }
    }
}
