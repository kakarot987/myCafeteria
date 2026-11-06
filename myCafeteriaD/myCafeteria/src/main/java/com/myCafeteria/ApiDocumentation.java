package com.myCafeteria;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Document supported public/customer/admin APIs while keeping legacy upload and
 * diagnostic handlers out of the focused documentation group.
 * Docs are public, but protected API operations still enforce JWT/roles.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
    title = "myCafeteria API",
    version = "1.0.0",
    description = "Cafeteria menu, JWT authentication, atomic checkout, and staff fulfillment."
))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
    scheme = "bearer", bearerFormat = "JWT")
public class ApiDocumentation {
    @Bean
    public GroupedOpenApi cafeteriaApi() {
        return GroupedOpenApi.builder()
            .group("cafeteria")
            .pathsToMatch("/api/auth/signin", "/api/auth/signup",
                          "/api/menu", "/api/menu/**",
                          "/api/orders", "/api/orders/**")
            .build();
    }
}
