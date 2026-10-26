package io.github.virolac.payments.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PaymentProcessor {
    private static final long PROCESSING_TIME_MILLIS = 3000;
    private static final int ONE_IN_N_FAILS = 6;

    public PaymentResult charge(BigDecimal amount) throws InterruptedException {
        Thread.sleep(PROCESSING_TIME_MILLIS);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextInt(ONE_IN_N_FAILS) == 0) {
            return PaymentResult.DECLINED;
        }

        return PaymentResult.SUCCEEDED;
    }
}
