package io.github.virolac.payments.payment;

import io.github.virolac.payments.messaging.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentListener {
    private final PaymentProcessor paymentProcessor;
    private final RabbitTemplate rabbitTemplate;

    public PaymentListener(PaymentProcessor paymentProcessor, RabbitTemplate rabbitTemplate) {
        this.paymentProcessor = paymentProcessor;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_PLACED_QUEUE)
    public void processPayment(OrderPlacedEvent orderPlacedEvent) throws InterruptedException {
        PaymentResult paymentResult = paymentProcessor.charge(orderPlacedEvent.total());

        rabbitTemplate.convertAndSend(
                RabbitConfig.PAYMENTS_EXCHANGE,
                RabbitConfig.PAYMENT_PROCESSED_ROUTING_KEY,
                new PaymentProcessedEvent(orderPlacedEvent.orderId(), paymentResult)
        );
    }
}
