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
    public static final String ORDERS_EXCHANGE = "orders";
    public static final String PAYMENTS_QUEUE = "payments.order-placed";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";

    public static final String ORDERS_DEAD_LETTER_EXCHANGE = "orders.dlx";
    public static final String PAYMENTS_DEAD_LETTER_QUEUE = "payments.order-placed.dlq";
    public static final String PAYMENTS_DEAD_LETTER_ROUTING_KEY = PAYMENTS_DEAD_LETTER_QUEUE;

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public Queue paymentsQueue() {
        return QueueBuilder
                .durable(PAYMENTS_QUEUE)
                .deadLetterExchange(ORDERS_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(PAYMENTS_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding paymentsBinding(TopicExchange ordersExchange, Queue paymentsQueue) {
        return BindingBuilder
                .bind(paymentsQueue)
                .to(ordersExchange)
                .with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    public DirectExchange ordersDeadLetterExchange() {
        return new DirectExchange(ORDERS_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue paymentsDeadLetterQueue() {
        return QueueBuilder.durable(PAYMENTS_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding paymentsDeadLetterBinding(
            DirectExchange ordersDeadLetterExchange,
            Queue paymentsDeadLetterQueue
    ) {
        return BindingBuilder
                .bind(paymentsDeadLetterQueue)
                .to(ordersDeadLetterExchange)
                .with(PAYMENTS_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
