package com.techconsulting.lending.config;

import com.techconsulting.lending.controller.AuthController;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void declaresGlobalJwtBearerAuthenticationAndKeepsAuthEndpointsPublic() {
        SecurityScheme scheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);

        assertThat(scheme).isNotNull();
        assertThat(scheme.name()).isEqualTo(OpenApiConfig.BEARER_AUTH);
        assertThat(scheme.type()).isEqualTo(SecuritySchemeType.HTTP);
        assertThat(scheme.scheme()).isEqualTo("bearer");
        assertThat(scheme.bearerFormat()).isEqualTo("JWT");
        assertThat(definition.security()).hasSize(1);
        assertThat(definition.security()[0].name()).isEqualTo(OpenApiConfig.BEARER_AUTH);
        assertThat(AuthController.class.getAnnotation(SecurityRequirements.class)).isNotNull();
    }
}
