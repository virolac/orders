package io.github.virolac.orders.payment;

import io.github.virolac.orders.RabbitMQTestcontainersConfiguration;
import io.github.virolac.orders.TestcontainersConfiguration;
import io.github.virolac.orders.order.OrderItemRequest;
import io.github.virolac.orders.order.OrderResponse;
import io.github.virolac.orders.order.OrderService;
import io.github.virolac.orders.order.OrderStatus;
import io.github.virolac.orders.order.PlaceOrderRequest;
import io.github.virolac.orders.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Import({TestcontainersConfiguration.class, RabbitMQTestcontainersConfiguration.class})
class PaymentFlowIntegrationTest {
    private static final long TIMEOUT_MILLIS = 10_000;
    private static final long POLL_INTERVAL_MILLIS = 100;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

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
