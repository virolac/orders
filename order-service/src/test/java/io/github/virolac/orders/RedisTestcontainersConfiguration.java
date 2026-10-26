package io.github.virolac.orders;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

/**
 * Test configuration that starts a throwaway Redis container and connects
 * the application's Redis client to it, so tests never use the development cache.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestcontainersConfiguration {
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:8").withExposedPorts(6379);
    }
}
