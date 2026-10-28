package io.github.virolac.orders.order;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.getOrdersForCustomer(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.getOrderById(id, jwt.getSubject());
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PlaceOrderRequest placeOrderRequest
    ) {
        OrderResponse placedOrder = orderService.placeOrder(jwt.getSubject(), placeOrderRequest);
        URI orderUri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(placedOrder.id())
                .toUri();

        return ResponseEntity.created(orderUri).body(placedOrder);
    }
}
