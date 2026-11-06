package com.vetcare.pacientes.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Claves y tokens generados solo para las pruebas.
 */
public final class TokensPrueba {

    private static final KeyPair CLAVES = generarClaves();
    private static final KeyPair OTRAS_CLAVES = generarClaves();

    private TokensPrueba() {
    }

    public static String clavePublica() {
        return Base64.getEncoder().encodeToString(CLAVES.getPublic().getEncoded());
    }

    public static String clavePrivada() {
        return Base64.getEncoder().encodeToString(CLAVES.getPrivate().getEncoded());
    }

    public static String token(String rol) {
        return firmar(CLAVES, rol, null, Instant.now().plus(Duration.ofMinutes(30)));
    }

    public static String tokenDePropietario(String propietarioId) {
        return firmar(CLAVES, "PROPIETARIO", propietarioId, Instant.now().plus(Duration.ofMinutes(30)));
    }

    public static String tokenExpirado(String rol) {
        return firmar(CLAVES, rol, null, Instant.now().minus(Duration.ofHours(1)));
    }

    public static String tokenConOtraClave(String rol) {
        return firmar(OTRAS_CLAVES, rol, null, Instant.now().plus(Duration.ofMinutes(30)));
    }

    private static String firmar(KeyPair claves, String rol, String propietarioId, Instant expira) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) claves.getPublic())
                .privateKey((RSAPrivateKey) claves.getPrivate())
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject("usuario-prueba")
                .claim("correo", "prueba@vetcare.test")
                .claim("rol", rol)
                .issuedAt(expira.minus(Duration.ofMinutes(30)))
                .expiresAt(expira);
        if (propietarioId != null) {
            claims.claim("propietarioId", propietarioId);
        }
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
    }

    private static KeyPair generarClaves() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
