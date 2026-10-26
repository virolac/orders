package io.github.virolac.orders.order;

import io.github.virolac.orders.messaging.RabbitConfig;
import io.github.virolac.orders.outbox.OutboxEvent;
import io.github.virolac.orders.outbox.OutboxEventRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OrderEventPublisher(OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    public void publishOrderPlaced(OrderPlacedEvent orderPlacedEvent) {
        OutboxEvent outboxEvent = new OutboxEvent(
                RabbitConfig.ORDERS_EXCHANGE,
                RabbitConfig.ORDER_PLACED_ROUTING_KEY,
                orderPlacedEvent.getClass().getName(),
                jsonMapper.writeValueAsString(orderPlacedEvent)
        );

        outboxEventRepository.save(outboxEvent);
    }
}
