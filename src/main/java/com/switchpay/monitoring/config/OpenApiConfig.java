package com.switchpay.monitoring.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI setup. Open /swagger-ui.html, click "Authorize"
 * and paste the token from POST /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI switchPayOpenApi() {
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("SwitchPay API")
                        .version("v1")
                        .description("Multi-channel banking and payment switch (ATM, mobile, merchant web)"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, jwtScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
