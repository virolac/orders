package io.github.virolac.orders.order;

import io.github.virolac.orders.product.Product;
import io.github.virolac.orders.product.ProductNotFoundException;
import io.github.virolac.orders.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderEventPublisher orderEventPublisher
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForCustomer(String customerId) {
        return orderRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String customerId) {
        return orderRepository
                .findByIdAndCustomerId(id, customerId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public OrderResponse placeOrder(String customerId, PlaceOrderRequest placeOrderRequest) {
        Map<Long, Integer> quantitiesByProductId = mergeQuantities(placeOrderRequest);
        Map<Long, Product> productsById = loadProducts(quantitiesByProductId);

        Order order = new Order(customerId);
        quantitiesByProductId.forEach((productId, quantity) -> {
            order.addItem(new OrderItem(productsById.get(productId), quantity));
        });

        Order placedOrder = orderRepository.saveAndFlush(order);
        orderEventPublisher.publishOrderPlaced(
                new OrderPlacedEvent(placedOrder.getId(), placedOrder.getTotal()));

        return OrderResponse.from(placedOrder);
    }

    @Transactional
    public void markOrderPaid(Long id) {
        findPendingOrder(id).ifPresent(Order::markPaid);
    }

    @Transactional
    public void markOrderFailed(Long id) {
        findPendingOrder(id).ifPresent(Order::markFailed);
    }

    private Map<Long, Integer> mergeQuantities(PlaceOrderRequest placeOrderRequest) {
        return placeOrderRequest
                .items()
                .stream()
                .collect(Collectors.toMap(
                                OrderItemRequest::productId,
                                OrderItemRequest::quantity,
                                Integer::sum,
                                LinkedHashMap::new
                        )
                );
    }

    private Map<Long, Product> loadProducts(Map<Long, Integer> quantitiesByProductId) {
        Map<Long, Product> productsById = productRepository
                .findAllById(quantitiesByProductId.keySet())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Optional<Long> missingProductId = quantitiesByProductId
                .keySet()
                .stream()
                .filter(id -> !productsById.containsKey(id))
                .findFirst();

        if (missingProductId.isPresent()) {
            throw new ProductNotFoundException(missingProductId.get());
        }

        return productsById;
    }

    private Optional<Order> findPendingOrder(Long id) {
        Order order = orderRepository
                .findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != OrderStatus.PENDING) {
            LOGGER.info("Order {} is already {}; ignoring payment result", id, order.getStatus());
            return Optional.empty();
        }

        return Optional.of(order);
    }
}
