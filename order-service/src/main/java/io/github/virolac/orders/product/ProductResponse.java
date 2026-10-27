package io.github.virolac.orders.product;

import java.math.BigDecimal;

public record ProductResponse(Long id,
                              String name,
                              BigDecimal price,
                              String description,
                              String imageUrl) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl()
        );
    }
}
