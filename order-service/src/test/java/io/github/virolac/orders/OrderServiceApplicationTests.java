package io.github.virolac.orders;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RabbitMQTestcontainersConfiguration.class})
class OrderServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
