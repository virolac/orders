package io.github.virolac.orders.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceOrderRequest(@NotEmpty List<@Valid OrderItemRequest> items) {
}
