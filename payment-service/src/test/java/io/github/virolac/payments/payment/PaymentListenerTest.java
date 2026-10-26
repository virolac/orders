package io.github.virolac.payments.payment;

import io.github.virolac.payments.messaging.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentListenerTest {
    private static final OrderPlacedEvent EVENT = new OrderPlacedEvent(7L, new BigDecimal("34.97"));

    private final PaymentProcessor paymentProcessor = mock(PaymentProcessor.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final PaymentListener paymentListener = new PaymentListener(paymentProcessor, rabbitTemplate);

    @Test
    void succeededPaymentPublishesSucceededResult() throws Exception {
        given(paymentProcessor.charge(new BigDecimal("34.97"))).willReturn(PaymentResult.SUCCEEDED);

        paymentListener.processPayment(EVENT);

        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.PAYMENTS_EXCHANGE,
                RabbitConfig.PAYMENT_PROCESSED_ROUTING_KEY,
                new PaymentProcessedEvent(7L, PaymentResult.SUCCEEDED));
    }

    @Test
    void declinedPaymentPublishesDeclinedResult() throws Exception {
        given(paymentProcessor.charge(new BigDecimal("34.97"))).willReturn(PaymentResult.DECLINED);

        paymentListener.processPayment(EVENT);

        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.PAYMENTS_EXCHANGE,
                RabbitConfig.PAYMENT_PROCESSED_ROUTING_KEY,
                new PaymentProcessedEvent(7L, PaymentResult.DECLINED));
    }
}
