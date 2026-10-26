package io.github.virolac.payments;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(RabbitMQTestcontainersConfiguration.class)
class PaymentServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
