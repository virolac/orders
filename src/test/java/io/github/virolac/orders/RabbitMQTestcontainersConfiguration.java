package io.github.virolac.orders;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Test configuration that starts a throwaway RabbitMQ container and connects
 * the application's RabbitMQ client to it, so tests never use the development broker.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RabbitMQTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitMQContainer() {
        return new RabbitMQContainer("rabbitmq:4-management");
    }
}
