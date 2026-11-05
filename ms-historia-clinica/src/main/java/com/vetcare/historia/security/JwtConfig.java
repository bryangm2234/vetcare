package com.vetcare.historia.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Valida los JWT con RS256 y la clave pública; ms-pacientes es el único servicio que los firma (D2).
 */
@Configuration
public class JwtConfig {

    @Bean
    public RSAPublicKey clavePublicaJwt(@Value("${vetcare.jwt.public-key:}") String clave) throws GeneralSecurityException {
        if (clave.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno JWT_PUBLIC_KEY");
        }
        byte[] der = Base64.getDecoder().decode(clave.trim());
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey clavePublicaJwt) {
        return NimbusJwtDecoder.withPublicKey(clavePublicaJwt).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        converter.setPrincipalClaimName("correo");
        return converter;
    }
}
