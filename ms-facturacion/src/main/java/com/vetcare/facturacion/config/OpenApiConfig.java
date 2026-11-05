package com.vetcare.facturacion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vetcareOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VetCare - ms-facturacion")
                .description("Tarifas, facturas, pagos, abonos, caja y reportes financieros.")
                .version("0.0.1"));
    }
}
