package io.github.virolac.orders.order;

import java.math.BigDecimal;

public record OrderPlacedEvent(Long orderId, BigDecimal total) {
}
