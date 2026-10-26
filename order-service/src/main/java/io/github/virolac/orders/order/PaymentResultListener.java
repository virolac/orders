package io.github.virolac.orders.order;

import io.github.virolac.orders.messaging.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {
    private final OrderService orderService;

    public PaymentResultListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_PROCESSED_QUEUE)
    public void paymentProcessed(PaymentProcessedEvent paymentProcessedEvent) {
        if (paymentProcessedEvent.paymentResult() == PaymentResult.SUCCEEDED) {
            orderService.markOrderPaid(paymentProcessedEvent.orderId());
        } else {
            orderService.markOrderFailed(paymentProcessedEvent.orderId());
        }
    }
}
