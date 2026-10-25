package io.github.virolac.orders.order;

import io.github.virolac.orders.TestcontainersConfiguration;
import io.github.virolac.orders.product.Product;
import io.github.virolac.orders.product.ProductInUseException;
import io.github.virolac.orders.product.ProductNotFoundException;
import io.github.virolac.orders.product.ProductRepository;
import io.github.virolac.orders.product.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest
@Import({TestcontainersConfiguration.class, OrderService.class, ProductService.class})
class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    private Product mascara;
    private Product powder;
    private Product lipstick;

    @BeforeEach
    void loadSampleProducts() {
        mascara = productRepository.findByName("Essence Mascara Lash Princess").getFirst();
        powder = productRepository.findByName("Powder Canister").getFirst();
        lipstick = productRepository.findByName("Red Lipstick").getFirst();
    }

    @Test
    void placeOrderReturnsPendingOrderWithItemsAndTotal() {
        OrderResponse response = orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(mascara.getId(), 2),
                new OrderItemRequest(powder.getId(), 1)
        )));

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.items())
                .extracting(OrderItemResponse::productId)
                .containsExactly(mascara.getId(), powder.getId());
        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo("9.99");
        assertThat(response.items().getFirst().lineTotal()).isEqualByComparingTo("19.98");
        assertThat(response.total()).isEqualByComparingTo("34.97");
    }

    @Test
    void placeOrderPublishesOrderPlacedEvent() {
        OrderResponse response = orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(mascara.getId(), 2),
                new OrderItemRequest(powder.getId(), 1)
        )));

        verify(orderEventPublisher).publishOrderPlaced(new OrderPlacedEvent(response.id(), response.total()));
    }

    @Test
    void placeOrderMergesDuplicateProducts() {
        OrderResponse response = orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(lipstick.getId(), 1),
                new OrderItemRequest(mascara.getId(), 2),
                new OrderItemRequest(lipstick.getId(), 2)
        )));

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).productId()).isEqualTo(lipstick.getId());
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
        assertThat(response.items().get(1).productId()).isEqualTo(mascara.getId());
        assertThat(response.items().get(1).quantity()).isEqualTo(2);
    }

    @Test
    void placeOrderKeepsUnitPriceWhenProductPriceChangesLater() {
        OrderResponse placed = placeOrderFor(lipstick);

        lipstick.setPrice(new BigDecimal("99.99"));
        entityManager.flush();
        entityManager.clear();

        OrderResponse reloaded = orderService.getOrderById(placed.id());

        assertThat(reloaded.items().getFirst().unitPrice()).isEqualByComparingTo("12.99");
    }

    @Test
    void placeOrderWithMissingProductThrowsSavesNothingAndPublishesNothing() {
        long ordersBefore = orderRepository.count();

        assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(lipstick.getId(), 1),
                new OrderItemRequest(999_999L, 1)
        )))).isInstanceOf(ProductNotFoundException.class);

        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
        verifyNoInteractions(orderEventPublisher);
    }

    @Test
    void getOrderByIdReturnsItemsInOriginalOrder() {
        OrderResponse placed = orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(powder.getId(), 1),
                new OrderItemRequest(lipstick.getId(), 1),
                new OrderItemRequest(mascara.getId(), 1)
        )));
        entityManager.flush();
        entityManager.clear();

        OrderResponse reloaded = orderService.getOrderById(placed.id());

        assertThat(reloaded.items())
                .extracting(OrderItemResponse::productId)
                .containsExactly(powder.getId(), lipstick.getId(), mascara.getId());
    }

    @Test
    void getOrderByIdThrowsForMissingOrder() {
        assertThatThrownBy(() -> orderService.getOrderById(999_999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getAllOrdersIncludesPlacedOrderWithItems() {
        OrderResponse placed = placeOrderFor(mascara);
        entityManager.flush();
        entityManager.clear();

        List<OrderResponse> orders = orderService.getAllOrders();

        assertThat(orders)
                .filteredOn(order -> order.id().equals(placed.id()))
                .singleElement()
                .satisfies(order -> assertThat(order.items())
                        .extracting(OrderItemResponse::productName)
                        .containsExactly("Essence Mascara Lash Princess"));
    }

    @Test
    void markOrderPaidSetsStatusToPaid() {
        OrderResponse placed = placeOrderFor(lipstick);

        orderService.markOrderPaid(placed.id());

        assertThat(reloadStatus(placed.id())).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void markOrderFailedSetsStatusToFailed() {
        OrderResponse placed = placeOrderFor(lipstick);

        orderService.markOrderFailed(placed.id());

        assertThat(reloadStatus(placed.id())).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void secondPaymentResultForSameOrderIsIgnored() {
        OrderResponse placed = placeOrderFor(lipstick);
        orderService.markOrderPaid(placed.id());

        orderService.markOrderFailed(placed.id());

        assertThat(reloadStatus(placed.id())).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void markOrderPaidThrowsForMissingOrder() {
        assertThatThrownBy(() -> orderService.markOrderPaid(999_999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void databaseRejectsSameProductTwiceInOneOrder() {
        Order order = new Order();
        order.addItem(new OrderItem(lipstick, 1));
        order.addItem(new OrderItem(lipstick, 2));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(order))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingProductInAnOrderThrowsProductInUse() {
        placeOrderFor(lipstick);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> productService.deleteProduct(lipstick.getId()))
                .isInstanceOf(ProductInUseException.class);
    }

    private OrderResponse placeOrderFor(Product product) {
        return orderService.placeOrder(new PlaceOrderRequest(List.of(
                new OrderItemRequest(product.getId(), 1)
        )));
    }

    private OrderStatus reloadStatus(Long orderId) {
        entityManager.flush();
        entityManager.clear();

        return orderService.getOrderById(orderId).status();
    }
}
