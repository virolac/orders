package io.github.virolac.orders.order;

import io.github.virolac.orders.product.Product;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId,
                                String productName,
                                int quantity,
                                BigDecimal unitPrice,
                                BigDecimal lineTotal) {
    public static OrderItemResponse from(OrderItem orderItem) {
        Product product = orderItem.getProduct();

        return new OrderItemResponse(
                product.getId(),
                product.getName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getLineTotal()
        );
    }
}
