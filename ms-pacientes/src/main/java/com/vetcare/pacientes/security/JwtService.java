package com.vetcare.pacientes.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.vetcare.pacientes.model.Usuario;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Firma los JWT con RS256. Este es el único servicio que conoce la clave privada (D2).
 */
@Service
public class JwtService {

    private final NimbusJwtEncoder encoder;
    private final Duration duracion;

    public JwtService(RSAPublicKey clavePublicaJwt,
                      @Value("${vetcare.jwt.private-key:}") String clavePrivada,
                      @Value("${vetcare.jwt.expiracion-minutos:30}") long minutos) throws GeneralSecurityException {
        if (clavePrivada.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno JWT_PRIVATE_KEY");
        }
        byte[] der = Base64.getDecoder().decode(clavePrivada.trim());
        RSAPrivateKey privada = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        RSAKey jwk = new RSAKey.Builder(clavePublicaJwt).privateKey(privada).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        this.duracion = Duration.ofMinutes(minutos);
    }

    public Jwt emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(usuario.getId())
                .claim("correo", usuario.getCorreo())
                .claim("rol", usuario.getRol().name())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracion));
        if (usuario.getPropietarioId() != null) {
            claims.claim("propietarioId", usuario.getPropietarioId());
        }
        if (usuario.getProveedorId() != null) {
            claims.claim("proveedorId", usuario.getProveedorId());
        }
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims.build()));
    }
}
