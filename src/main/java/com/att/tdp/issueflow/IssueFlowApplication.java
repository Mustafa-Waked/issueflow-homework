package com.att.tdp.issueflow;

import com.att.tdp.issueflow.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Import(OpenApiConfig.class)
@OpenAPIDefinition(security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME))
public class IssueFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(IssueFlowApplication.class, args);
	}

}
