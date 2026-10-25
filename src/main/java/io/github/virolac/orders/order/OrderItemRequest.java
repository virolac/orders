package io.github.virolac.orders.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(@NotNull Long productId,
                               @NotNull @Positive @Max(1000) Integer quantity) {
}
