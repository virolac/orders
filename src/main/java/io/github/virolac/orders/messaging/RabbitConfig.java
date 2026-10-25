package io.github.virolac.orders.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RabbitConfig {
    public static final String ORDERS_EXCHANGE = "orders";
    public static final String PAYMENTS_QUEUE = "payments.order-placed";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public Queue paymentsQueue() {
        return new Queue(PAYMENTS_QUEUE);
    }

    @Bean
    public Binding paymentsBinding(TopicExchange ordersExchange, Queue paymentsQueue) {
        return BindingBuilder
                .bind(paymentsQueue)
                .to(ordersExchange)
                .with(ORDER_PLACED_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
