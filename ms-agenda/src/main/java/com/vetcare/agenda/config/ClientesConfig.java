package com.vetcare.agenda.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientesConfig {

    @Bean
    @Scope("prototype")
    public RestClient.Builder restClientBuilder(@Value("${vetcare.servicios.timeout-segundos:10}") long segundos) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory();
        fabrica.setReadTimeout(Duration.ofSeconds(segundos));
        return RestClient.builder().requestFactory(fabrica);
    }
}
