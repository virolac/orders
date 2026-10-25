package io.github.virolac.orders.order;

import io.github.virolac.orders.product.Product;
import io.github.virolac.orders.product.ProductNotFoundException;
import io.github.virolac.orders.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest placeOrderRequest) {
        Map<Long, Integer> quantitiesByProductId = mergeQuantities(placeOrderRequest);
        Map<Long, Product> productsById = loadProducts(quantitiesByProductId);

        Order order = new Order();
        quantitiesByProductId.forEach((productId, quantity) -> {
            order.addItem(new OrderItem(productsById.get(productId), quantity));
        });

        Order placedOrder = orderRepository.saveAndFlush(order);

        return OrderResponse.from(placedOrder);
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
}
