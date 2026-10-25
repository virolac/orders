package io.github.virolac.orders.order;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super(String.format("Order with id %d not found", id));
    }
}
