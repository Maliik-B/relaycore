package dev.maliik.relaycore.common.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * OpenAPI document metadata and the {@code bearerAuth} security scheme, which gives Swagger UI an
 * "Authorize" button for pasting a JWT obtained from {@code /auth/login}.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "relaycore API",
        version = "v1",
        description = "Game online-services backend — players/auth, matchmaking, inventory, leaderboards."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
