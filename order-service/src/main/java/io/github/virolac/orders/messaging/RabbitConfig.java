package io.github.virolac.orders.messaging;

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
    public static final String PAYMENTS_EXCHANGE = "payments";
    public static final String PAYMENT_PROCESSED_QUEUE = "orders.payment-processed";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";
    public static final String PAYMENT_PROCESSED_DEAD_LETTER_QUEUE = "orders.payment-processed.dlq";
    public static final String PAYMENT_PROCESSED_DEAD_LETTER_ROUTING_KEY = PAYMENT_PROCESSED_DEAD_LETTER_QUEUE;

    public static final String ORDERS_EXCHANGE = "orders";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";
    public static final String ORDERS_DEAD_LETTER_EXCHANGE = "orders.dlx";

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENTS_EXCHANGE);
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder
                .durable(PAYMENT_PROCESSED_QUEUE)
                .deadLetterExchange(ORDERS_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(PAYMENT_PROCESSED_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding paymentProcessedBinding(
            TopicExchange paymentsExchange,
            Queue paymentProcessedQueue
    ) {
        return BindingBuilder
                .bind(paymentProcessedQueue)
                .to(paymentsExchange)
                .with(PAYMENT_PROCESSED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public DirectExchange ordersDeadLetterExchange() {
        return new DirectExchange(ORDERS_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue paymentProcessedDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENT_PROCESSED_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding paymentProcessedDeadLetterBinding(
            DirectExchange ordersDeadLetterExchange,
            Queue paymentProcessedDeadLetterQueue
    ) {
        return BindingBuilder
                .bind(paymentProcessedDeadLetterQueue)
                .to(ordersDeadLetterExchange)
                .with(PAYMENT_PROCESSED_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
