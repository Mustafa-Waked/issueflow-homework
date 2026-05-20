package com.att.tdp.issueflow.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT bearer security for Swagger UI. Merges into springdoc-generated OpenAPI only —
 * never replaces {@link Components} (that would drop schemas and break $ref resolvers).
 */
@Configuration
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste JWT from POST /auth/login (value only, without 'Bearer ' prefix)."
)
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI issueFlowOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("IssueFlow API")
                        .version("1.0.0")
                        .description("Authenticate via POST /auth/login, then Authorize with the JWT."));
        applyBearerSecurity(openAPI);
        return openAPI;
    }

    @Bean
    public OperationCustomizer bearerAuthOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(
                    io.swagger.v3.oas.annotations.security.SecurityRequirements.class)) {
                operation.setSecurity(java.util.List.of());
                return operation;
            }
            operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
            return operation;
        };
    }

    /**
     * Adds bearer scheme and global security without removing springdoc-generated schemas.
     */
    static void applyBearerSecurity(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }

        io.swagger.v3.oas.models.security.SecurityScheme bearerScheme =
                new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT access token from POST /auth/login");
        components.addSecuritySchemes(BEARER_AUTH_SCHEME, bearerScheme);

        boolean hasGlobalBearer = openApi.getSecurity() != null && openApi.getSecurity().stream()
                .anyMatch(req -> req.containsKey(BEARER_AUTH_SCHEME));
        if (!hasGlobalBearer) {
            openApi.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
        }
    }
}
