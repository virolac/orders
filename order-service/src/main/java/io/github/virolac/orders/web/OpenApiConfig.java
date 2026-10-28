package io.github.virolac.orders.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "Order Service API",
                version = "0.0.1",
                description = "Products, orders and payment status for the orders demo."
        )
)
@SecurityScheme(
        name = OpenApiConfig.SECURITY_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT"
)
public class OpenApiConfig {
    public static final String SECURITY_SCHEME = "keycloak";
}
