package com.techconsulting.lending.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Lending Dashboard API",
                version = "v1",
                description = "Portfolio, report upload and email reconciliation APIs"),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter the JWT returned by /api/v1/auth/login. Swagger adds the Bearer prefix automatically.")
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";

    private OpenApiConfig() {
    }
}
