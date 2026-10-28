package io.github.virolac.orders.order;

import io.github.virolac.orders.RabbitMQTestcontainersConfiguration;
import io.github.virolac.orders.RedisTestcontainersConfiguration;
import io.github.virolac.orders.TestcontainersConfiguration;
import io.github.virolac.orders.messaging.RabbitConfig;
import io.github.virolac.orders.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({
        TestcontainersConfiguration.class,
        RabbitMQTestcontainersConfiguration.class,
        RedisTestcontainersConfiguration.class
})
class PaymentResultIntegrationTest {
    private static final long TIMEOUT_MILLIS = 10_000;
    private static final long POLL_INTERVAL_MILLIS = 100;
    private static final String CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void succeededPaymentResultMarksOrderPaid() throws Exception {
        OrderResponse placed = placeOrderForRedLipstick();

        sendPaymentProcessed(placed.id(), PaymentResult.SUCCEEDED);

        assertThat(awaitFinalStatus(placed.id())).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void declinedPaymentResultMarksOrderFailed() throws Exception {
        OrderResponse placed = placeOrderForRedLipstick();

        sendPaymentProcessed(placed.id(), PaymentResult.DECLINED);

        assertThat(awaitFinalStatus(placed.id())).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void paymentResultForMissingOrderIsDeadLettered() {
        sendPaymentProcessed(999_999L, PaymentResult.SUCCEEDED);

        Message deadLetter = rabbitTemplate.receive(RabbitConfig.PAYMENT_PROCESSED_DEAD_LETTER_QUEUE, TIMEOUT_MILLIS);
        assertThat(deadLetter).isNotNull();

        List<Map<String, ?>> xDeath = deadLetter.getMessageProperties().getXDeathHeader();
        assertThat(xDeath).isNotEmpty();
        assertThat(xDeath.getFirst().get("queue")).hasToString(RabbitConfig.PAYMENT_PROCESSED_QUEUE);
    }

    private void sendPaymentProcessed(Long orderId, PaymentResult result) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.PAYMENTS_EXCHANGE,
                RabbitConfig.PAYMENT_PROCESSED_ROUTING_KEY,
                new PaymentProcessedEvent(orderId, result));
    }

    private OrderResponse placeOrderForRedLipstick() {
        Long lipstickId = productRepository.findByName("Red Lipstick").getFirst().getId();

        return orderService.placeOrder(CUSTOMER_ID, new PlaceOrderRequest(List.of(
                new OrderItemRequest(lipstickId, 1)
        )));
    }

    private OrderStatus awaitFinalStatus(Long orderId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;

        while (System.currentTimeMillis() < deadline) {
            OrderStatus status = orderService.getOrderById(orderId, CUSTOMER_ID).status();
            if (status != OrderStatus.PENDING) {
                return status;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }

        throw new AssertionError("Order " + orderId + " was still PENDING after " + TIMEOUT_MILLIS + " ms");
    }
}
