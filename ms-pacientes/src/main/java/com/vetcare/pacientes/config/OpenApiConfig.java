package com.vetcare.pacientes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vetcareOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VetCare - ms-pacientes")
                .description("Usuarios e inicio de sesión (D2), propietarios, mascotas, alergias y estado.")
                .version("0.0.1"));
    }
}
