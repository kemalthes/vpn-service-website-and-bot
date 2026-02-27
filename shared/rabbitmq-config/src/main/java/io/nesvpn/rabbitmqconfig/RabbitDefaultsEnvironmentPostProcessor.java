package io.nesvpn.rabbitmqconfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

@Slf4j
public class RabbitDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
        try {
            List<PropertySource<?>> propertySources = yamlPropertySourceLoader.load("rabbitmq-defaults",
                    new ClassPathResource("rabbitmq-defaults.yml"));
            environment.getPropertySources().addLast(propertySources.getFirst());
        } catch (IOException e) {
                log.error("Failed to load RabbitMQ defaults.yml", e);
            throw new RuntimeException(e);
        }
    }
}
