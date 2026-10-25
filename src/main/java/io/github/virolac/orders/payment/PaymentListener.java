package io.github.virolac.orders.payment;

import io.github.virolac.orders.messaging.RabbitConfig;
import io.github.virolac.orders.order.OrderPlacedEvent;
import io.github.virolac.orders.order.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentListener {
    private final PaymentProcessor paymentProcessor;
    private final OrderService orderService;

    public PaymentListener(PaymentProcessor paymentProcessor, OrderService orderService) {
        this.paymentProcessor = paymentProcessor;
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENTS_QUEUE)
    public void processPayment(OrderPlacedEvent orderPlacedEvent) throws InterruptedException {
        if (paymentProcessor.charge(orderPlacedEvent.total()) == PaymentResult.SUCCEEDED) {
            orderService.markOrderPaid(orderPlacedEvent.orderId());
        } else {
            orderService.markOrderFailed(orderPlacedEvent.orderId());
        }
    }
}
