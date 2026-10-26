package io.github.virolac.payments.payment;

public record PaymentProcessedEvent(Long orderId, PaymentResult paymentResult) {
}
