package com.localmarket.main.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

@Configuration
public class SwaggerConfig {
    final String securitySchemeName = "cookie";

    @Bean
    public OpenAPI localMarketOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Local Market API")
                .version("1.0")
                .description("API documentation for Local Market application")
                .contact(new Contact()
                    .name("Local Market Team")
                    .email("support@localmarket.com")))
            .externalDocs(new ExternalDocumentation()
                .description("Local Market API GitHub Repository")
                .url("https://github.com/aymanbest/Local-market-backend"))
            .addSecurityItem(new SecurityRequirement()
                .addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name("JWT")
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .scheme("cookie")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
}