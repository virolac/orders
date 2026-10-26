package io.github.virolac.payments.payment;

import java.math.BigDecimal;

public record OrderPlacedEvent(Long orderId, BigDecimal total) {
}
