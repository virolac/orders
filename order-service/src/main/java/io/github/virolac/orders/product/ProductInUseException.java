package io.github.virolac.orders.product;

public class ProductInUseException extends RuntimeException {
    public ProductInUseException(Long id) {
        super(String.format("Product with id %d is in use", id));
    }
}
