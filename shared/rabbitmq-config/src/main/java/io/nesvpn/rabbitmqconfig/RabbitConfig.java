package io.nesvpn.rabbitmqconfig;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

@EnableRabbit
@AutoConfiguration
@AutoConfigureAfter(RabbitAutoConfiguration.class)
public class RabbitConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new JacksonJsonMessageConverter());
        template.setMandatory(true);
        template.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
        return template;
    }

    @Bean
    public Queue linkRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dlx.link");
        args.put("x-dead-letter-routing-key", "dlx.link.request");
        return new Queue("link.request", true, false, false, args);
    }

    @Bean
    public Queue linkResponseQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dlx.link");
        args.put("x-dead-letter-routing-key", "dlx.link.response");
        return new Queue("link.response", true, false, false, args);
    }

    @Bean
    public Queue dlqRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 30000);
        args.put("x-dead-letter-exchange", "exchange.link");
        args.put("x-dead-letter-routing-key", "link.request");
        return new Queue("dlq.link.request", true, false, false, args);
    }

    @Bean
    public Queue dlqResponseQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 30000);
        args.put("x-dead-letter-exchange", "exchange.link");
        args.put("x-dead-letter-routing-key", "link.response");
        return new Queue("dlq.link.response", true, false, false, args);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.link");
    }

    @Bean
    public DirectExchange linkExchange() {
        return new DirectExchange("exchange.link");
    }

    @Bean
    public Binding linkRequestBinding() {
        return BindingBuilder.bind(linkRequestQueue())
                .to(linkExchange())
                .with("link.request");
    }

    @Bean
    public Binding linkResponseBinding() {
        return BindingBuilder.bind(linkResponseQueue())
                .to(linkExchange())
                .with("link.response");
    }

    @Bean
    public Binding dlqRequestBinding() {
        return BindingBuilder.bind(dlqRequestQueue())
                .to(dlxExchange())
                .with("dlx.link.request");
    }

    @Bean
    public Binding dlqResponseBinding() {
        return BindingBuilder.bind(dlqResponseQueue())
                .to(dlxExchange())
                .with("dlx.link.response");
    }
}
