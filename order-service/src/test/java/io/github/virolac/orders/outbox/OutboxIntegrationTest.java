package io.github.virolac.orders.outbox;

import io.github.virolac.orders.RabbitMQTestcontainersConfiguration;
import io.github.virolac.orders.TestcontainersConfiguration;
import io.github.virolac.orders.messaging.RabbitConfig;
import io.github.virolac.orders.order.OrderItemRequest;
import io.github.virolac.orders.order.OrderPlacedEvent;
import io.github.virolac.orders.order.OrderResponse;
import io.github.virolac.orders.order.OrderService;
import io.github.virolac.orders.order.PlaceOrderRequest;
import io.github.virolac.orders.product.ProductNotFoundException;
import io.github.virolac.orders.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RabbitMQTestcontainersConfiguration.class})
class OutboxIntegrationTest {
    private static final long TIMEOUT_MILLIS = 10_000;
    private static final long POLL_INTERVAL_MILLIS = 100;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void placingOrderWritesOutboxEventThatRelayPublishes() throws Exception {
        OrderResponse placed = placeOrderForRedLipstick();

        OutboxEvent event = findEventForOrder(placed.id());
        assertThat(event.getExchange()).isEqualTo(RabbitConfig.ORDERS_EXCHANGE);
        assertThat(event.getRoutingKey()).isEqualTo(RabbitConfig.ORDER_PLACED_ROUTING_KEY);
        assertThat(event.getEventType()).isEqualTo(OrderPlacedEvent.class.getName());
        assertThat(event.getCreatedAt()).isNotNull();

        assertThat(awaitPublished(event.getId()).getPublishedAt()).isNotNull();
    }

    @Test
    void failedOrderWritesNoOutboxEvent() {
        long eventsBefore = outboxEventRepository.count();
        Long lipstickId = productRepository.findByName("Red Lipstick").getFirst().getId();

        assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(lipstickId, 1),
                new OrderItemRequest(999_999L, 1)
        )))).isInstanceOf(ProductNotFoundException.class);

        assertThat(outboxEventRepository.count()).isEqualTo(eventsBefore);
    }

    private OrderResponse placeOrderForRedLipstick() {
        Long lipstickId = productRepository.findByName("Red Lipstick").getFirst().getId();

        return orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(lipstickId, 1)
        )));
    }

    private OutboxEvent findEventForOrder(Long orderId) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> jsonMapper.readTree(event.getPayload()).get("orderId").asLong() == orderId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No outbox event for order " + orderId));
    }

    private OutboxEvent awaitPublished(Long eventId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;

        while (System.currentTimeMillis() < deadline) {
            OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
            if (event.getPublishedAt() != null) {
                return event;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }

        throw new AssertionError("Outbox event " + eventId + " was not published after " + TIMEOUT_MILLIS + " ms");
    }
}
