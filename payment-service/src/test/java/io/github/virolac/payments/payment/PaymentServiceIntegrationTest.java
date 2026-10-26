package io.github.virolac.payments.payment;

import io.github.virolac.payments.RabbitMQTestcontainersConfiguration;
import io.github.virolac.payments.messaging.RabbitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@Import(RabbitMQTestcontainersConfiguration.class)
class PaymentServiceIntegrationTest {
    private static final long TIMEOUT_MILLIS = 10_000;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private PaymentProcessor paymentProcessor;

    private String resultQueue;

    @BeforeEach
    void bindTemporaryResultQueue() {
        Queue queue = amqpAdmin.declareQueue();
        assert queue != null;
        amqpAdmin.declareBinding(BindingBuilder
                .bind(queue)
                .to(new TopicExchange(RabbitConfig.PAYMENTS_EXCHANGE))
                .with(RabbitConfig.PAYMENT_PROCESSED_ROUTING_KEY));
        resultQueue = queue.getName();
    }

    @Test
    void orderPlacedEventProducesSucceededResult() throws Exception {
        given(paymentProcessor.charge(any())).willReturn(PaymentResult.SUCCEEDED);

        sendOrderPlaced(7L);

        JsonNode result = receiveResult();
        assertThat(result.get("orderId").asLong()).isEqualTo(7L);
        assertThat(result.get("paymentResult").asString()).isEqualTo("SUCCEEDED");
    }

    @Test
    void orderPlacedEventProducesDeclinedResult() throws Exception {
        given(paymentProcessor.charge(any())).willReturn(PaymentResult.DECLINED);

        sendOrderPlaced(8L);

        JsonNode result = receiveResult();
        assertThat(result.get("orderId").asLong()).isEqualTo(8L);
        assertThat(result.get("paymentResult").asString()).isEqualTo("DECLINED");
    }

    @Test
    void failingPaymentIsRetriedThenDeadLetteredWithoutResult() throws Exception {
        given(paymentProcessor.charge(any())).willThrow(new IllegalStateException("Payment provider unavailable"));

        sendOrderPlaced(9L);

        Message deadLetter = rabbitTemplate.receive(RabbitConfig.ORDER_PLACED_DEAD_LETTER_QUEUE, TIMEOUT_MILLIS);
        assertThat(deadLetter).isNotNull();

        List<Map<String, ?>> xDeath = deadLetter.getMessageProperties().getXDeathHeader();
        assertThat(xDeath).isNotEmpty();
        assertThat(xDeath.getFirst().get("queue")).hasToString(RabbitConfig.ORDER_PLACED_QUEUE);

        verify(paymentProcessor, times(3)).charge(any());
        assertThat(rabbitTemplate.receive(resultQueue)).isNull();
    }

    @Test
    void malformedMessageIsDeadLetteredWithoutReachingProcessor() {
        Message malformed = MessageBuilder
                .withBody("{\"orderId\": ".getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        rabbitTemplate.send(RabbitConfig.ORDERS_EXCHANGE, RabbitConfig.ORDER_PLACED_ROUTING_KEY, malformed);

        Message deadLetter = rabbitTemplate.receive(RabbitConfig.ORDER_PLACED_DEAD_LETTER_QUEUE, TIMEOUT_MILLIS);
        assertThat(deadLetter).isNotNull();
        verifyNoInteractions(paymentProcessor);
    }

    private void sendOrderPlaced(Long orderId) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDERS_EXCHANGE,
                RabbitConfig.ORDER_PLACED_ROUTING_KEY,
                new OrderPlacedEvent(orderId, new BigDecimal("34.97")));
    }

    private JsonNode receiveResult() {
        Message message = rabbitTemplate.receive(resultQueue, TIMEOUT_MILLIS);
        assertThat(message).isNotNull();

        return jsonMapper.readTree(message.getBody());
    }
}
