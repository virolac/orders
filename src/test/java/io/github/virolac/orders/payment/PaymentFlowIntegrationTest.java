package io.github.virolac.orders.payment;

import io.github.virolac.orders.RabbitMQTestcontainersConfiguration;
import io.github.virolac.orders.TestcontainersConfiguration;
import io.github.virolac.orders.messaging.RabbitConfig;
import io.github.virolac.orders.order.OrderItemRequest;
import io.github.virolac.orders.order.OrderPlacedEvent;
import io.github.virolac.orders.order.OrderResponse;
import io.github.virolac.orders.order.OrderService;
import io.github.virolac.orders.order.OrderStatus;
import io.github.virolac.orders.order.PlaceOrderRequest;
import io.github.virolac.orders.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RabbitMQTestcontainersConfiguration.class})
class PaymentFlowIntegrationTest {
    private static final long TIMEOUT_MILLIS = 10_000;
    private static final long POLL_INTERVAL_MILLIS = 100;
    private static final long MISSING_ORDER_ID = 999_999L;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private PaymentProcessor paymentProcessor;

    @Test
    void placedOrderBecomesPaidWhenPaymentSucceeds() throws Exception {
        given(paymentProcessor.charge(any())).willReturn(PaymentResult.SUCCEEDED);

        OrderResponse placed = placeOrderForRedLipstick();

        assertThat(placed.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(awaitFinalStatus(placed.id())).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void placedOrderBecomesFailedWhenPaymentIsDeclined() throws Exception {
        given(paymentProcessor.charge(any())).willReturn(PaymentResult.DECLINED);

        OrderResponse placed = placeOrderForRedLipstick();

        assertThat(placed.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(awaitFinalStatus(placed.id())).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void eventForMissingOrderIsRetriedThenDeadLettered() throws Exception {
        given(paymentProcessor.charge(any())).willReturn(PaymentResult.SUCCEEDED);

        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDERS_EXCHANGE,
                RabbitConfig.ORDER_PLACED_ROUTING_KEY,
                new OrderPlacedEvent(MISSING_ORDER_ID, new BigDecimal("12.99")));

        Message deadLetter = rabbitTemplate.receive(RabbitConfig.PAYMENTS_DEAD_LETTER_QUEUE, TIMEOUT_MILLIS);

        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8))
                .contains(String.valueOf(MISSING_ORDER_ID));

        List<Map<String, ?>> xDeath = deadLetter.getMessageProperties().getXDeathHeader();
        assertThat(xDeath).isNotEmpty();
        assertThat(xDeath.getFirst().get("queue")).hasToString(RabbitConfig.PAYMENTS_QUEUE);
        assertThat(xDeath.getFirst().get("reason")).hasToString("rejected");

        verify(paymentProcessor, times(3)).charge(any());
    }

    @Test
    void malformedMessageIsDeadLetteredWithoutReachingListener() {
        Message malformed = MessageBuilder
                .withBody("{\"orderId\": ".getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        rabbitTemplate.send(RabbitConfig.ORDERS_EXCHANGE, RabbitConfig.ORDER_PLACED_ROUTING_KEY, malformed);

        Message deadLetter = rabbitTemplate.receive(RabbitConfig.PAYMENTS_DEAD_LETTER_QUEUE, TIMEOUT_MILLIS);

        assertThat(deadLetter).isNotNull();
        verifyNoInteractions(paymentProcessor);
    }

    private OrderResponse placeOrderForRedLipstick() {
        Long lipstickId = productRepository.findByName("Red Lipstick").getFirst().getId();

        return orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(lipstickId, 1)
        )));
    }

    private OrderStatus awaitFinalStatus(Long orderId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;

        while (System.currentTimeMillis() < deadline) {
            OrderStatus status = orderService.getOrderById(orderId).status();
            if (status != OrderStatus.PENDING) {
                return status;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }

        throw new AssertionError("Order " + orderId + " was still PENDING after " + TIMEOUT_MILLIS + " ms");
    }
}
