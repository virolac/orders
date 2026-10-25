package io.github.virolac.orders.order;

import io.github.virolac.orders.messaging.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderPlaced(OrderPlacedEvent orderPlacedEvent) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDERS_EXCHANGE, RabbitConfig.ORDER_PLACED_ROUTING_KEY, orderPlacedEvent);
    }
}
