package com.example.transcriber.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration for the Free and Dirty Transcriber REST API.
 * Documents API versioning (header-based), JWT authentication, and endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        final String apiVersionHeader = "API-Version";

        return new OpenAPI()
            .info(new Info()
                .title("Free and Dirty Transcriber API")
                .version("1.0")
                .description("REST API for audio file transcription using free and open-source libraries. "
                    + "Supports file upload, status tracking, rate limiting, and result delivery. "
                    + "Use header **API-Version: v1** for versioning.")
                .contact(new Contact().name("API Support")))
            .servers(List.of(
                new Server().url(contextPath).description("API base path")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token from authentication service")));
    }
}
