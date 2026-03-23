package com.example.licenseplate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI licensePlateOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("License Plate Management API")
                .description("API для управления заявлениями на номерные знаки")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Dev Team")
                    .email("support@licenseplate.com")
                    .url("https://licenseplate.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://springdoc.org")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Server"),
                new Server().url("https://api.licenseplate.com").description("Production Server")
            ));
    }
}