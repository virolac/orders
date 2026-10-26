package io.github.virolac.orders.order;

public record PaymentProcessedEvent(Long orderId, PaymentResult paymentResult) {
}
