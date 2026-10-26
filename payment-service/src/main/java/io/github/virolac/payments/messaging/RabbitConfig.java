package io.github.virolac.payments.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RabbitConfig {
    public static final String ORDERS_EXCHANGE = "orders";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";
    public static final String ORDER_PLACED_QUEUE = "payments.order-placed";
    public static final String ORDER_PLACED_DEAD_LETTER_QUEUE = "payments.order-placed.dlq";
    public static final String ORDER_PLACED_DEAD_LETTER_ROUTING_KEY = ORDER_PLACED_DEAD_LETTER_QUEUE;

    public static final String PAYMENTS_EXCHANGE = "payments";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";
    public static final String PAYMENTS_DEAD_LETTER_EXCHANGE = "payments.dlx";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder
                .durable(ORDER_PLACED_QUEUE)
                .deadLetterExchange(PAYMENTS_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ORDER_PLACED_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderPlacedBinding(TopicExchange ordersExchange, Queue orderPlacedQueue) {
        return BindingBuilder
                .bind(orderPlacedQueue)
                .to(ordersExchange)
                .with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENTS_EXCHANGE);
    }

    @Bean
    public DirectExchange paymentsDeadLetterExchange() {
        return new DirectExchange(PAYMENTS_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue orderPlacedDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_PLACED_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding orderPlacedDeadLetterBinding(
            DirectExchange paymentsDeadLetterExchange,
            Queue orderPlacedDeadLetterQueue
    ) {
        return BindingBuilder
                .bind(orderPlacedDeadLetterQueue)
                .to(paymentsDeadLetterExchange)
                .with(ORDER_PLACED_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
