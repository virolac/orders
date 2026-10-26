package io.github.virolac.orders.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String VALID_BODY = """
            {
              "name": "Blue Eyeliner",
              "price": 7.49
            }
            """;

    private static final String INVALID_BODY = """
            {
              "price": -10.99
            }
            """;

    private static final ProductRequest VALID_REQUEST =
            new ProductRequest("Blue Eyeliner", new BigDecimal("7.49"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void getProductsReturnsAllProducts() throws Exception {
        given(productService.getAllProducts()).willReturn(List.of(
                new ProductResponse(1L, "Red Lipstick", new BigDecimal("12.99")),
                new ProductResponse(51L, "Powder Canister", new BigDecimal("14.99"))
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Red Lipstick"))
                .andExpect(jsonPath("$[0].price").value(12.99))
                .andExpect(jsonPath("$[1].id").value(51));
    }

    @Test
    void getProductReturnsProduct() throws Exception {
        given(productService.getProductById(1L))
                .willReturn(new ProductResponse(1L, "Red Lipstick", new BigDecimal("12.99")));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Red Lipstick"))
                .andExpect(jsonPath("$.price").value(12.99));
    }

    @Test
    void getMissingProductReturnsNotFoundProblemDetail() throws Exception {
        given(productService.getProductById(99L)).willThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product with id 99 not found"));
    }

    @Test
    void createProductReturnsCreatedWithLocation() throws Exception {
        given(productService.createProduct(VALID_REQUEST))
                .willReturn(new ProductResponse(102L, "Blue Eyeliner", new BigDecimal("7.49")));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/products/102")))
                .andExpect(jsonPath("$.id").value(102))
                .andExpect(jsonPath("$.name").value("Blue Eyeliner"))
                .andExpect(jsonPath("$.price").value(7.49));
    }

    @Test
    void createInvalidProductReturnsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.price").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void createWithMalformedJsonReturnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(productService);
    }

    @Test
    void updateProductReturnsUpdatedProduct() throws Exception {
        given(productService.updateProduct(1L, VALID_REQUEST))
                .willReturn(new ProductResponse(1L, "Blue Eyeliner", new BigDecimal("7.49")));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Blue Eyeliner"))
                .andExpect(jsonPath("$.price").value(7.49));
    }

    @Test
    void updateMissingProductReturnsNotFound() throws Exception {
        given(productService.updateProduct(99L, VALID_REQUEST)).willThrow(new ProductNotFoundException(99L));

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void updateInvalidProductReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.price").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void deleteProductReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    void deleteMissingProductReturnsNotFound() throws Exception {
        willThrow(new ProductNotFoundException(99L)).given(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
