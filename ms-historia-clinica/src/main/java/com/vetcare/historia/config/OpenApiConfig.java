package com.vetcare.historia.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vetcareOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VetCare - ms-historia-clinica")
                .description("Consultas, signos vitales, vacunación, laboratorio, fórmulas y hospitalización.")
                .version("0.0.1"));
    }
}
