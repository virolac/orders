package io.github.virolac.orders.web;

import io.github.virolac.orders.RabbitMQTestcontainersConfiguration;
import io.github.virolac.orders.RedisTestcontainersConfiguration;
import io.github.virolac.orders.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TestcontainersConfiguration.class,
        RabbitMQTestcontainersConfiguration.class,
        RedisTestcontainersConfiguration.class
})
class OpenApiDocsIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsAreReachableWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Order Service API"))
                .andExpect(jsonPath("$.info.version").value("0.0.1"));
    }

    @Test
    void apiDocsDescribeBearerSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.keycloak.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.keycloak.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.keycloak.bearerFormat").value("JWT"));
    }

    @Test
    void apiDocsGroupEndpointsByTag() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/orders'].get.tags[0]").value("Orders"))
                .andExpect(jsonPath("$.paths['/api/orders'].post.tags[0]").value("Orders"))
                .andExpect(jsonPath("$.paths['/api/products'].get.tags[0]").value("Products"))
                .andExpect(jsonPath("$.paths['/api/products/{id}'].delete.tags[0]").value("Products"));
    }

    @Test
    void orderEndpointsRequireTheSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/orders'].get.security[0].keycloak").isArray())
                .andExpect(jsonPath("$.paths['/api/orders'].post.security[0].keycloak").isArray())
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].get.security[0].keycloak").isArray());
    }

    @Test
    void productWritesRequireTheSecuritySchemeButReadsDoNot() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/products'].post.security[0].keycloak").isArray())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].put.security[0].keycloak").isArray())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].delete.security[0].keycloak").isArray())
                .andExpect(jsonPath("$.paths['/api/products'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].get.security").doesNotExist());
    }

    @Test
    void swaggerUiIsReachableWithoutToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
