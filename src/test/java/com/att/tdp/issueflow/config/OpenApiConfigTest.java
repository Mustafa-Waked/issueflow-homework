package com.att.tdp.issueflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiConfigTest {

    @Autowired
    private OpenAPI openAPI;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiBeanContainsBearerSecurityScheme() {
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        var scheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_AUTH_SCHEME);
        assertNotNull(scheme);
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
        assertNotNull(openAPI.getSecurity());
        assertFalse(openAPI.getSecurity().isEmpty());
        assertTrue(openAPI.getSecurity().stream()
                .anyMatch(req -> req.containsKey(OpenApiConfig.BEARER_AUTH_SCHEME)));
    }

    @Test
    void apiDocsExposeBearerAuthForSwaggerUi() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.components.schemas.ApiError").exists())
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/projects'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("#/schemas/"), "Broken schema refs must use #/components/schemas/");
        assertTrue(body.contains("#/components/schemas/ApiError"));
    }
}
