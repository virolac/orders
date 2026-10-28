package io.github.virolac.orders.product;

import io.github.virolac.orders.security.KeycloakRealmRoleConverter;
import io.github.virolac.orders.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {
    private static final String DESCRIPTION = "A soft blue eyeliner.";
    private static final String IMAGE_URL = "https://cdn.example.com/blue-eyeliner.webp";

    private static final String VALID_BODY = """
            {
              "name": "Blue Eyeliner",
              "price": 7.49,
              "description": "A soft blue eyeliner.",
              "imageUrl": "https://cdn.example.com/blue-eyeliner.webp"
            }
            """;

    private static final String INVALID_BODY = """
            {
              "price": -10.99
            }
            """;

    private static final ProductRequest VALID_REQUEST =
            new ProductRequest("Blue Eyeliner", new BigDecimal("7.49"), DESCRIPTION, IMAGE_URL);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getProductsReturnsAllProductsWithoutToken() throws Exception {
        given(productService.getAllProducts()).willReturn(List.of(
                new ProductResponse(1L, "Red Lipstick", new BigDecimal("12.99"), DESCRIPTION, IMAGE_URL),
                new ProductResponse(51L, "Powder Canister", new BigDecimal("14.99"), null, null)
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Red Lipstick"))
                .andExpect(jsonPath("$[0].price").value(12.99))
                .andExpect(jsonPath("$[0].description").value(DESCRIPTION))
                .andExpect(jsonPath("$[0].imageUrl").value(IMAGE_URL))
                .andExpect(jsonPath("$[1].id").value(51))
                .andExpect(jsonPath("$[1].description").value(nullValue()))
                .andExpect(jsonPath("$[1].imageUrl").value(nullValue()));
    }

    @Test
    void getProductReturnsProductWithoutToken() throws Exception {
        given(productService.getProductById(1L))
                .willReturn(new ProductResponse(1L, "Red Lipstick", new BigDecimal("12.99"), DESCRIPTION, IMAGE_URL));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Red Lipstick"))
                .andExpect(jsonPath("$.price").value(12.99))
                .andExpect(jsonPath("$.description").value(DESCRIPTION))
                .andExpect(jsonPath("$.imageUrl").value(IMAGE_URL));
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
                .willReturn(new ProductResponse(102L, "Blue Eyeliner", new BigDecimal("7.49"), DESCRIPTION, IMAGE_URL));

        mockMvc.perform(post("/api/products")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/products/102")))
                .andExpect(jsonPath("$.id").value(102))
                .andExpect(jsonPath("$.name").value("Blue Eyeliner"))
                .andExpect(jsonPath("$.price").value(7.49))
                .andExpect(jsonPath("$.description").value(DESCRIPTION))
                .andExpect(jsonPath("$.imageUrl").value(IMAGE_URL));
    }

    @Test
    void createProductWithoutDescriptionAndImageIsAccepted() throws Exception {
        ProductRequest request = new ProductRequest("Blue Eyeliner", new BigDecimal("7.49"), null, null);
        given(productService.createProduct(request))
                .willReturn(new ProductResponse(102L, "Blue Eyeliner", new BigDecimal("7.49"), null, null));

        mockMvc.perform(post("/api/products")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blue Eyeliner",
                                  "price": 7.49
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.imageUrl").value(nullValue()));
    }

    @Test
    void createProductWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    @Test
    void createProductAsCustomerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    void createInvalidProductReturnsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(admin())
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
    void createProductWithJavascriptImageUrlReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithImageUrl("javascript:alert(1)")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.imageUrl").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void createProductWithHttpImageUrlReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithImageUrl("http://cdn.example.com/blue-eyeliner.webp")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.imageUrl").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void createProductWithTooLongDescriptionReturnsBadRequest() throws Exception {
        String body = """
                {
                  "name": "Blue Eyeliner",
                  "price": 7.49,
                  "description": "%s"
                }
                """.formatted("a".repeat(1001));

        mockMvc.perform(post("/api/products")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void createWithMalformedJsonReturnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(admin())
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
                .willReturn(new ProductResponse(1L, "Blue Eyeliner", new BigDecimal("7.49"), DESCRIPTION, IMAGE_URL));

        mockMvc.perform(put("/api/products/1")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Blue Eyeliner"))
                .andExpect(jsonPath("$.price").value(7.49))
                .andExpect(jsonPath("$.description").value(DESCRIPTION))
                .andExpect(jsonPath("$.imageUrl").value(IMAGE_URL));
    }

    @Test
    void updateProductAsCustomerReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .with(customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    void updateMissingProductReturnsNotFound() throws Exception {
        given(productService.updateProduct(99L, VALID_REQUEST)).willThrow(new ProductNotFoundException(99L));

        mockMvc.perform(put("/api/products/99")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void updateInvalidProductReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isArray())
                .andExpect(jsonPath("$.errors.price").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void deleteProductReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/1").with(admin()))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    void deleteProductWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    @Test
    void deleteMissingProductReturnsNotFound() throws Exception {
        willThrow(new ProductNotFoundException(99L)).given(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/99").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void deleteProductInUseReturnsConflict() throws Exception {
        willThrow(new ProductInUseException(1L)).given(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Product with id 1 is in use"));
    }

    private static RequestPostProcessor admin() {
        return withRealmRoles("admin");
    }

    private static RequestPostProcessor customer() {
        return withRealmRoles("customer");
    }

    private static RequestPostProcessor withRealmRoles(String... roles) {
        return jwt()
                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of(roles))))
                .authorities(new KeycloakRealmRoleConverter());
    }

    private static String bodyWithImageUrl(String imageUrl) {
        return """
                {
                  "name": "Blue Eyeliner",
                  "price": 7.49,
                  "imageUrl": "%s"
                }
                """.formatted(imageUrl);
    }
}
