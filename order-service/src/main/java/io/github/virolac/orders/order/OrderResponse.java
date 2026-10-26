package io.github.virolac.orders.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id,
                            OrderStatus status,
                            List<OrderItemResponse> items,
                            BigDecimal total,
                            Instant createdAt) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotal(),
                order.getCreatedAt()
        );
    }
}
