package io.github.virolac.orders.order;

import io.github.virolac.orders.web.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
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
@Tag(name = "Orders")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME)
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List my orders")
    public List<OrderResponse> getOrders(@AuthenticationPrincipal @NonNull Jwt jwt) {
        return orderService.getOrdersForCustomer(jwt.getSubject());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my orders")
    public OrderResponse getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal @NonNull Jwt jwt
    ) {
        return orderService.getOrderById(id, jwt.getSubject());
    }

    @PostMapping
    @Operation(summary = "Place an order")
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal @NonNull Jwt jwt,
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
