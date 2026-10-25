package io.github.virolac.orders.payment;

import io.github.virolac.orders.order.OrderPlacedEvent;
import io.github.virolac.orders.order.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentListenerTest {
    private static final OrderPlacedEvent EVENT = new OrderPlacedEvent(7L, new BigDecimal("34.97"));

    private final PaymentProcessor paymentProcessor = mock(PaymentProcessor.class);
    private final OrderService orderService = mock(OrderService.class);
    private final PaymentListener paymentListener = new PaymentListener(paymentProcessor, orderService);

    @Test
    void succeededPaymentMarksOrderPaid() throws Exception {
        given(paymentProcessor.charge(new BigDecimal("34.97"))).willReturn(PaymentResult.SUCCEEDED);

        paymentListener.processPayment(EVENT);

        verify(orderService).markOrderPaid(7L);
        verify(orderService, never()).markOrderFailed(7L);
    }

    @Test
    void declinedPaymentMarksOrderFailed() throws Exception {
        given(paymentProcessor.charge(new BigDecimal("34.97"))).willReturn(PaymentResult.DECLINED);

        paymentListener.processPayment(EVENT);

        verify(orderService).markOrderFailed(7L);
        verify(orderService, never()).markOrderPaid(7L);
    }
}
