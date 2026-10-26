package io.github.virolac.orders.order;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentResultListenerTest {
    private final OrderService orderService = mock(OrderService.class);
    private final PaymentResultListener paymentResultListener = new PaymentResultListener(orderService);

    @Test
    void succeededPaymentMarksOrderPaid() {
        paymentResultListener.paymentProcessed(new PaymentProcessedEvent(7L, PaymentResult.SUCCEEDED));

        verify(orderService).markOrderPaid(7L);
        verify(orderService, never()).markOrderFailed(7L);
    }

    @Test
    void declinedPaymentMarksOrderFailed() {
        paymentResultListener.paymentProcessed(new PaymentProcessedEvent(7L, PaymentResult.DECLINED));

        verify(orderService).markOrderFailed(7L);
        verify(orderService, never()).markOrderPaid(7L);
    }
}
