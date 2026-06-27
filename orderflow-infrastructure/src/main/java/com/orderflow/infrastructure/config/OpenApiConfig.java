package com.orderflow.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata and the bearer-JWT security scheme, so the generated Swagger
 * UI (/swagger-ui.html) has an "Authorize" button that sends the token.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "OrderFlow API", version = "v1",
    description = "Order management backend (hexagonal architecture, Java 21)."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
    scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
