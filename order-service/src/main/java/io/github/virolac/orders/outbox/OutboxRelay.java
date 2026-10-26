package io.github.virolac.orders.outbox;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OutboxRelay {
    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void sendUnpublishedMessages() {
        List<OutboxEvent> outboxEvents = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();

        for (OutboxEvent outboxEvent : outboxEvents) {
            Message message = MessageBuilder
                    .withBody(outboxEvent.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setHeader(DefaultClassMapper.DEFAULT_CLASSID_FIELD_NAME, outboxEvent.getEventType())
                    .build();
            rabbitTemplate.send(outboxEvent.getExchange(), outboxEvent.getRoutingKey(), message);

            outboxEvent.markPublished();
        }
    }
}
