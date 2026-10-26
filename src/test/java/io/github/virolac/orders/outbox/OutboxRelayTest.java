package io.github.virolac.orders.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OutboxRelayTest {
    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final OutboxRelay outboxRelay = new OutboxRelay(outboxEventRepository, rabbitTemplate);

    @Test
    void sendsEachEventWithItsOwnPayloadAndTypeAndMarksItPublished() {
        OutboxEvent first = new OutboxEvent("orders", "order.placed", "com.example.FirstEvent", "{\"id\":1}");
        OutboxEvent second = new OutboxEvent("orders", "order.paid", "com.example.SecondEvent", "{\"id\":2}");
        given(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()).willReturn(List.of(first, second));

        outboxRelay.sendUnpublishedMessages();

        ArgumentCaptor<String> exchanges = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeys = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, times(2)).send(exchanges.capture(), routingKeys.capture(), messages.capture());

        assertThat(exchanges.getAllValues()).containsExactly("orders", "orders");
        assertThat(routingKeys.getAllValues()).containsExactly("order.placed", "order.paid");

        Message firstMessage = messages.getAllValues().getFirst();
        Message secondMessage = messages.getAllValues().getLast();

        assertThat(new String(firstMessage.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"id\":1}");
        assertThat(new String(secondMessage.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"id\":2}");
        assertThat(firstMessage.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(secondMessage.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(firstMessage.getMessageProperties().<String>getHeader("__TypeId__")).isEqualTo("com.example.FirstEvent");
        assertThat(secondMessage.getMessageProperties().<String>getHeader("__TypeId__")).isEqualTo("com.example.SecondEvent");

        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNotNull();
    }

    @Test
    void failedSendPropagatesAndLeavesEventUnpublished() {
        OutboxEvent event = new OutboxEvent("orders", "order.placed", "com.example.FirstEvent", "{\"id\":1}");
        given(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()).willReturn(List.of(event));
        willThrow(new AmqpException("Broker unavailable"))
                .given(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        assertThatThrownBy(outboxRelay::sendUnpublishedMessages)
                .isInstanceOf(AmqpException.class);

        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void emptyBatchSendsNothing() {
        given(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByIdAsc()).willReturn(List.of());

        outboxRelay.sendUnpublishedMessages();

        verifyNoInteractions(rabbitTemplate);
    }
}
