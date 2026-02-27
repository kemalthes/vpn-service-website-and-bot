package io.nesvpn.rabbitmqconfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;

@Slf4j
@EnableRabbit
@AutoConfigureAfter(RabbitAutoConfiguration.class)
@AutoConfiguration
public class RabbitConfig {

    public static final String EXCHANGE = "exchange.link";
    public static final String DLX_EXCHANGE = "dlx.link";

    public static final String ROUTING_KEY_REQUEST = "link.request";
    public static final String ROUTING_KEY_DLX = "dlx.link.request";
    public static final String ROUTING_KEY_FAILED = "link.request.failed"; // Новый ключ для кладбища

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.addTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate linkRabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);

        // Обязательно для работы ReturnsCallback
        template.setMandatory(true);

        // 1. ОБРАБОТКА ПОДТВЕРЖДЕНИЙ ОТ БРОКЕРА (ConfirmCallback)
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // Брокер прислал NACK (например, переполнен диск на сервере RabbitMQ)
                // Здесь в идеале нужно кидать алерт в Grafana или Telegram разработчикам
                log.error("🚨 Брокер ОТКЛОНИЛ сообщение (NACK)! Причина: {}", cause);
            }
        });

        // 2. ОБРАБОТКА НЕМАРШРУТИЗИРУЕМЫХ СООБЩЕНИЙ (ReturnsCallback)
        template.setReturnsCallback(returned -> {
            log.error("⚠️ Сообщение потеряно! Exchange: {}, RoutingKey: {}, Причина: {}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });

        return template;
    }

    @Bean
    public DirectExchange linkExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // --- ОЧЕРЕДИ ---

    // 1. Основная очередь
    @Bean
    public Queue linkRequestQueue() {
        return QueueBuilder.durable(ROUTING_KEY_REQUEST)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLX)
                .build();
    }

    // 2. Очередь задержки (Крутится 30 сек и возвращается в основную)
    @Bean
    public Queue dlqRequestQueue() {
        return QueueBuilder.durable("dlq.link.request")
                .withArgument("x-message-ttl", 30000)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_REQUEST)
                .build();
    }

    // 3. ОЧЕРЕДЬ-КЛАДБИЩЕ (Для сообщений, исчерпавших лимит попыток)
    @Bean
    public Queue failedRequestQueue() {
        return QueueBuilder.durable("failed.link.request").build();
    }

    // --- БИНДИНГИ ---

    @Bean
    public Binding linkRequestBinding() {
        return BindingBuilder.bind(linkRequestQueue()).to(linkExchange()).with(ROUTING_KEY_REQUEST);
    }

    @Bean
    public Binding dlqRequestBinding() {
        return BindingBuilder.bind(dlqRequestQueue()).to(dlxExchange()).with(ROUTING_KEY_DLX);
    }

    @Bean
    public Binding failedRequestBinding() {
        // Биндим кладбище к DLX обменнику
        return BindingBuilder.bind(failedRequestQueue()).to(dlxExchange()).with(ROUTING_KEY_FAILED);
    }
}