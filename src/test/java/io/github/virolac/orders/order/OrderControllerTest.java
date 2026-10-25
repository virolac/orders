package io.github.virolac.orders.order;

import io.github.virolac.orders.product.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    private static final String VALID_BODY = """
            {
              "items": [
                { "productId": 1, "quantity": 2 },
                { "productId": 51, "quantity": 1 }
              ]
            }
            """;

    private static final PlaceOrderRequest VALID_REQUEST = new PlaceOrderRequest(List.of(
            new OrderItemRequest(1L, 2),
            new OrderItemRequest(51L, 1)
    ));

    private static final OrderResponse PLACED_ORDER = new OrderResponse(
            7L,
            OrderStatus.PENDING,
            List.of(
                    new OrderItemResponse(1L, "Essence Mascara Lash Princess", 2,
                            new BigDecimal("9.99"), new BigDecimal("19.98")),
                    new OrderItemResponse(51L, "Powder Canister", 1,
                            new BigDecimal("14.99"), new BigDecimal("14.99"))
            ),
            new BigDecimal("34.97"),
            Instant.parse("2026-10-25T18:00:00Z")
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void placeOrderReturnsCreatedWithLocationAndOrder() throws Exception {
        given(orderService.placeOrder(VALID_REQUEST)).willReturn(PLACED_ORDER);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/orders/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Essence Mascara Lash Princess"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(9.99))
                .andExpect(jsonPath("$.items[0].lineTotal").value(19.98))
                .andExpect(jsonPath("$.total").value(34.97))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void placeOrderWithNoItemsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.items").isArray());

        verifyNoInteractions(orderService);
    }

    @Test
    void placeOrderValidatesEachItem() throws Exception {
        String body = """
                {
                  "items": [
                    { "productId": 1, "quantity": 0 },
                    { "quantity": 1 },
                    { "productId": 51, "quantity": 1001 },
                    { "productId": 101 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors['items[0].quantity']").isArray())
                .andExpect(jsonPath("$.errors['items[1].productId']").isArray())
                .andExpect(jsonPath("$.errors['items[2].quantity']").isArray())
                .andExpect(jsonPath("$.errors['items[3].quantity']").isArray());

        verifyNoInteractions(orderService);
    }

    @Test
    void placeOrderWithBareArrayBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[ { \"productId\": 1, \"quantity\": 2 } ]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(orderService);
    }

    @Test
    void placeOrderWithMissingProductReturnsNotFound() throws Exception {
        given(orderService.placeOrder(VALID_REQUEST)).willThrow(new ProductNotFoundException(51L));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Product with id 51 not found"));
    }

    @Test
    void getOrderReturnsOrder() throws Exception {
        given(orderService.getOrderById(7L)).willReturn(PLACED_ORDER);

        mockMvc.perform(get("/api/orders/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total").value(34.97));
    }

    @Test
    void getMissingOrderReturnsNotFound() throws Exception {
        given(orderService.getOrderById(99L)).willThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Order with id 99 not found"));
    }

    @Test
    void getOrdersReturnsAllOrders() throws Exception {
        given(orderService.getAllOrders()).willReturn(List.of(PLACED_ORDER));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7));
    }
}
