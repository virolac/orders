package io.github.virolac.orders.product;

import java.math.BigDecimal;

public record ProductRequest(String name, BigDecimal price) {
}
