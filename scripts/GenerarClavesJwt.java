import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Agrega al archivo .env las claves JWT y el administrador inicial para desarrollo local.
 * Ejecutar desde la raíz del repositorio: java scripts/GenerarClavesJwt.java
 * El archivo .env está excluido de Git. No imprime ningún secreto.
 */
public class GenerarClavesJwt {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Path env = Path.of(".env");
        String actual = Files.exists(env) ? Files.readString(env) : "";
        if (actual.contains("JWT_PRIVATE_KEY=")) {
            System.out.println("El archivo .env ya tiene claves JWT. No se modificó.");
            return;
        }

        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair claves = generador.generateKeyPair();
        Base64.Encoder base64 = Base64.getEncoder();

        byte[] aleatorio = new byte[18];
        new SecureRandom().nextBytes(aleatorio);
        String contrasena = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);

        String lineas = (actual.isEmpty() || actual.endsWith("\n") ? "" : "\n")
                + "JWT_PUBLIC_KEY=" + base64.encodeToString(claves.getPublic().getEncoded()) + "\n"
                + "JWT_PRIVATE_KEY=" + base64.encodeToString(claves.getPrivate().getEncoded()) + "\n"
                + "VETCARE_ADMIN_CORREO=admin@vetcare.local\n"
                + "VETCARE_ADMIN_CONTRASENA=" + contrasena + "\n";
        Files.writeString(env, lineas, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        System.out.println("Claves y administrador inicial agregados a .env.");
    }
}
