package app.crewup;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API-wide OpenAPI settings (M0-9). Endpoints document themselves with annotations on their controllers (API-8); the
 * frontend's client is generated from the result, committed as {@code api/openapi.json}.
 */
@Configuration
class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI crewUpOpenApi() {
        return new OpenAPI()
                .info(new Info().title("CrewUp API").version("v1"))
                // Relative, so the contract doesn't change with the host or port the docs were fetched from.
                .servers(List.of(new Server().url("/")))
                // Every documented endpoint is under /api and needs an access token (M0-4); this also gives
                // Swagger UI its Authorize button.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
