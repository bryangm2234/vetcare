package com.vetcare.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vetcare.pacientes.config.ArranqueDatos;
import com.vetcare.pacientes.model.Usuario;
import com.vetcare.pacientes.repository.UsuarioRepository;
import com.vetcare.pacientes.security.Rol;
import com.vetcare.pacientes.security.TokensPrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Escenarios de HU-001 a HU-020 contra un MongoDB real, en una base de datos exclusiva de pruebas.
 * Solo corre si se define MONGODB_TEST_URI, como ocurre en GitHub Actions.
 */
@SpringBootTest(properties = "spring.mongodb.database=" + Sprint1IntegracionTest.BASE_DE_PRUEBA)
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MONGODB_TEST_URI", matches = ".+")
class Sprint1IntegracionTest {

    static final String BASE_DE_PRUEBA = "vetcare_pacientes_test";
    private static final String CORREO_ADMIN = "admin@vetcare.test";
    private static final String CONTRASENA_ADMIN = "Admin.Prueba.123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ArranqueDatos arranqueDatos;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.mongodb.uri", () -> System.getenv("MONGODB_TEST_URI"));
        registro.add("vetcare.jwt.public-key", TokensPrueba::clavePublica);
        registro.add("vetcare.jwt.private-key", TokensPrueba::clavePrivada);
        registro.add("vetcare.admin.correo", () -> CORREO_ADMIN);
        registro.add("vetcare.admin.contrasena", () -> CONTRASENA_ADMIN);
    }

    @BeforeEach
    void prepararBase() {
        limpiarBase();
        arranqueDatos.preparar();
    }

    @AfterEach
    void limpiarBase() {
        assertThat(mongoTemplate.getDb().getName()).isEqualTo(BASE_DE_PRUEBA);
        mongoTemplate.getDb().drop();
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, String cuerpo) throws Exception {
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mockMvc.perform(peticion);
    }

    private String tokenDe(String rol) {
        return TokensPrueba.token(rol);
    }

    private String login(String correo, String contrasena) throws Exception {
        String respuesta = enviar(post("/api/auth/login"), null, """
                {"correo":"%s","contrasena":"%s"}""".formatted(correo, contrasena))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String id(ResultActions acciones) throws Exception {
        return JsonPath.read(acciones.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String crearPropietario(String documento, String nombre) throws Exception {
        return id(enviar(post("/api/propietarios"), tokenDe("RECEPCIONISTA"), """
                {"documento":"%s","nombre":"%s","telefono":"300","correo":"p@correo.test","direccion":"Calle 10"}"""
                .formatted(documento, nombre)).andExpect(status().isCreated()));
    }

    private String crearMascota(String propietarioId, String especie, String raza) throws Exception {
        return id(enviar(post("/api/mascotas"), tokenDe("RECEPCIONISTA"), """
                {"nombre":"Max","especie":"%s","raza":"%s","sexo":"MACHO","fechaNacimiento":"2022-03-15","pesoKg":12.5,"propietarioId":"%s"}"""
                .formatted(especie, raza, propietarioId)).andExpect(status().isCreated()));
    }

    private String crearUsuario(String correo, String contrasena, String rol) throws Exception {
        String admin = login(CORREO_ADMIN, CONTRASENA_ADMIN);
        return id(enviar(post("/api/usuarios"), admin, """
                {"nombre":"Ana","correo":"%s","contrasena":"%s","rol":"%s"}""".formatted(correo, contrasena, rol))
                .andExpect(status().isCreated()));
    }

    @Test
    void hu001_administradorInicialYCreacionDeUsuarios() throws Exception {
        Usuario admin = usuarioRepository.findByCorreo(CORREO_ADMIN).orElseThrow();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(admin.getContrasenaHash()).isNotEqualTo(CONTRASENA_ADMIN);

        String token = login(CORREO_ADMIN, CONTRASENA_ADMIN);
        String cuerpo = """
                {"nombre":"Ana","correo":"ana@vetcare.test","contrasena":"clave-segura","rol":"CAJERO"}""";
        enviar(post("/api/usuarios"), token, cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
        assertThat(passwordEncoder.matches("clave-segura", usuarioRepository.findByCorreo("ana@vetcare.test").orElseThrow().getContrasenaHash())).isTrue();

        enviar(post("/api/usuarios"), token, cuerpo)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado"));
    }

    @Test
    void hu001_administradorExistenteNoSeModifica() {
        long antes = usuarioRepository.count();
        String hash = usuarioRepository.findByCorreo(CORREO_ADMIN).orElseThrow().getContrasenaHash();

        arranqueDatos.preparar();

        assertThat(usuarioRepository.count()).isEqualTo(antes);
        assertThat(usuarioRepository.findByCorreo(CORREO_ADMIN).orElseThrow().getContrasenaHash()).isEqualTo(hash);
    }

    @Test
    void hu001_propietarioConVinculoInicioSesionYConsultaSoloSuMascota() throws Exception {
        String propietario = crearPropietario("109", "María Gómez");
        String otro = crearPropietario("110", "Luis Díaz");
        String propia = crearMascota(propietario, "Canino", "Criollo");
        String ajena = crearMascota(otro, "Felino", "Persa");
        String admin = login(CORREO_ADMIN, CONTRASENA_ADMIN);

        enviar(post("/api/usuarios"), admin, """
                {"nombre":"María","correo":"maria@vetcare.test","contrasena":"clave-segura","rol":"PROPIETARIO"}""")
                .andExpect(status().isUnprocessableContent());
        enviar(post("/api/usuarios"), admin, """
                {"nombre":"María","correo":"maria@vetcare.test","contrasena":"clave-segura","rol":"PROPIETARIO","propietarioId":"%s"}"""
                .formatted(propietario)).andExpect(status().isCreated());

        String token = login("maria@vetcare.test", "clave-segura");
        enviar(get("/api/mascotas/" + propia), token, null).andExpect(status().isOk());
        enviar(get("/api/mascotas/" + ajena), token, null).andExpect(status().isForbidden());
    }

    @Test
    void hu002_hu004_hu009_accesoDesactivacionYAuditoria() throws Exception {
        String usuario = crearUsuario("cajero@vetcare.test", "clave-segura", "CAJERO");
        login("cajero@vetcare.test", "clave-segura");

        enviar(post("/api/auth/login"), null, """
                {"correo":"cajero@vetcare.test","contrasena":"incorrecta"}""")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));

        String admin = login(CORREO_ADMIN, CONTRASENA_ADMIN);
        enviar(patch("/api/usuarios/" + usuario + "/desactivar"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
        enviar(post("/api/auth/login"), null, """
                {"correo":"cajero@vetcare.test","contrasena":"clave-segura"}""")
                .andExpect(status().isUnauthorized());
        enviar(patch("/api/usuarios/no-existe/desactivar"), admin, null).andExpect(status().isNotFound());

        enviar(get("/api/auditoria?accion=INICIO_SESION&usuario=cajero@vetcare.test"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.resultado == 'FALLIDO')]").isNotEmpty())
                .andExpect(jsonPath("$.contenido[0].contrasena").doesNotExist());
        enviar(get("/api/auditoria?accion=DESACTIVAR_USUARIO"), admin, null)
                .andExpect(jsonPath("$.contenido[0].usuario").value(CORREO_ADMIN))
                .andExpect(jsonPath("$.contenido[0].recurso").value("usuarios/" + usuario));
    }

    @Test
    void hu004_noPuedeDesactivarseASiMismo() throws Exception {
        String admin = login(CORREO_ADMIN, CONTRASENA_ADMIN);
        String id = usuarioRepository.findByCorreo(CORREO_ADMIN).orElseThrow().getId();

        enviar(patch("/api/usuarios/" + id + "/desactivar"), admin, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("No puede desactivar su propio usuario"));
    }

    @Test
    void hu005_cambioDeContrasena() throws Exception {
        crearUsuario("cajero@vetcare.test", "clave-segura", "CAJERO");
        String token = login("cajero@vetcare.test", "clave-segura");

        enviar(put("/api/usuarios/me/contrasena"), token, """
                {"contrasenaActual":"incorrecta","contrasenaNueva":"clave-nueva-1"}""")
                .andExpect(status().isUnprocessableContent());
        login("cajero@vetcare.test", "clave-segura");

        enviar(put("/api/usuarios/me/contrasena"), token, """
                {"contrasenaActual":"clave-segura","contrasenaNueva":"clave-nueva-1"}""")
                .andExpect(status().isOk());
        login("cajero@vetcare.test", "clave-nueva-1");
        enviar(post("/api/auth/login"), null, """
                {"correo":"cajero@vetcare.test","contrasena":"clave-segura"}""")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hu006_filtraPorRolYEstadoSinExponerLaContrasena() throws Exception {
        String inactivo = crearUsuario("cajero1@vetcare.test", "clave-segura", "CAJERO");
        crearUsuario("cajero2@vetcare.test", "clave-segura", "CAJERO");
        crearUsuario("recepcion@vetcare.test", "clave-segura", "RECEPCIONISTA");
        String admin = login(CORREO_ADMIN, CONTRASENA_ADMIN);
        enviar(patch("/api/usuarios/" + inactivo + "/desactivar"), admin, null).andExpect(status().isOk());

        enviar(get("/api/usuarios?rol=CAJERO&estado=ACTIVO"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].correo").value("cajero2@vetcare.test"))
                .andExpect(jsonPath("$.contenido[0].contrasenaHash").doesNotExist());
        enviar(get("/api/usuarios?rol=FARMACIA"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    void hu008_usaLaBaseDePrueba() throws Exception {
        crearPropietario("109", "María Gómez");
        assertThat(mongoTemplate.getDb().getName()).isEqualTo(BASE_DE_PRUEBA);
        assertThat(mongoTemplate.collectionExists("propietarios")).isTrue();
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void hu011_documentoDuplicado() throws Exception {
        crearPropietario("109", "María Gómez");

        enviar(post("/api/propietarios"), tokenDe("RECEPCIONISTA"), """
                {"documento":"109","nombre":"Otra","telefono":"300","correo":"o@correo.test","direccion":"Calle 1"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El documento ya está registrado"));
    }

    @Test
    void hu012_mascotaConPropietarioInexistente() throws Exception {
        enviar(post("/api/mascotas"), tokenDe("RECEPCIONISTA"), """
                {"nombre":"Max","especie":"Canino","raza":"Criollo","sexo":"MACHO","fechaNacimiento":"2022-03-15","pesoKg":12.5,"propietarioId":"no-existe"}""")
                .andExpect(status().isNotFound());
    }

    @Test
    void hu013_buscaPorDocumentoYNombreSinTildes() throws Exception {
        crearPropietario("109", "María Gómez");
        crearPropietario("110", "Luis Díaz");
        String token = tokenDe("RECEPCIONISTA");

        enviar(get("/api/propietarios?documento=109"), token, null)
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].nombre").value("María Gómez"));
        enviar(get("/api/propietarios?nombre=gomez"), token, null)
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].nombre").value("María Gómez"));
        enviar(get("/api/propietarios?nombre=.*"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)));
    }

    @Test
    void hu014_actualizaContacto() throws Exception {
        String id = crearPropietario("109", "María Gómez");

        enviar(patch("/api/propietarios/" + id + "/contacto"), tokenDe("RECEPCIONISTA"), """
                {"telefono":"311","correo":"nuevo@correo.test","direccion":"Carrera 5"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("311"))
                .andExpect(jsonPath("$.documento").value("109"));
    }

    @Test
    void hu015_hu017_mascotasConSuEstado() throws Exception {
        String propietario = crearPropietario("109", "María Gómez");
        String vacio = crearPropietario("110", "Luis Díaz");
        crearMascota(propietario, "Canino", "Criollo");
        String inactiva = crearMascota(propietario, "Felino", "Persa");
        String token = tokenDe("RECEPCIONISTA");

        enviar(patch("/api/mascotas/" + inactiva + "/estado"), token, """
                {"estado":"INACTIVA"}""").andExpect(status().isOk());

        enviar(get("/api/propietarios/" + propietario + "/mascotas"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.estado == 'INACTIVA')]", hasSize(1)));
        enviar(get("/api/propietarios/" + vacio + "/mascotas"), token, null).andExpect(jsonPath("$", hasSize(0)));
        enviar(get("/api/propietarios/no-existe/mascotas"), token, null).andExpect(status().isNotFound());
    }

    @Test
    void hu016_antecedentesVisiblesAlConsultar() throws Exception {
        String mascota = crearMascota(crearPropietario("109", "María Gómez"), "Canino", "Criollo");

        enviar(put("/api/mascotas/" + mascota + "/antecedentes"), tokenDe("VETERINARIO"), """
                {"alergias":["Penicilina"],"condicionesCronicas":["Diabetes"]}""").andExpect(status().isOk());

        enviar(get("/api/mascotas/" + mascota), tokenDe("VETERINARIO"), null)
                .andExpect(jsonPath("$.alergias[0]").value("Penicilina"))
                .andExpect(jsonPath("$.condicionesCronicas[0]").value("Diabetes"));
    }

    @Test
    void hu017_hu018_fallecimientoYTransferencia() throws Exception {
        String anterior = crearPropietario("109", "María Gómez");
        String nuevo = crearPropietario("110", "Luis Díaz");
        String mascota = crearMascota(anterior, "Canino", "Criollo");
        String token = tokenDe("RECEPCIONISTA");

        enviar(post("/api/mascotas/" + mascota + "/transferencia"), token, """
                {"nuevoPropietarioId":"no-existe"}""").andExpect(status().isNotFound());
        enviar(post("/api/mascotas/" + mascota + "/transferencia"), token, """
                {"nuevoPropietarioId":"%s"}""".formatted(nuevo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propietarioId").value(nuevo))
                .andExpect(jsonPath("$.transferencias[0].propietarioAnteriorId").value(anterior));
        enviar(get("/api/propietarios/" + nuevo + "/mascotas"), token, null).andExpect(jsonPath("$", hasSize(1)));

        enviar(patch("/api/mascotas/" + mascota + "/estado"), token, """
                {"estado":"FALLECIDA","fechaFallecimiento":"2026-11-01"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLECIDA"));
        enviar(patch("/api/mascotas/" + mascota + "/estado"), token, """
                {"estado":"ACTIVA"}""").andExpect(status().isUnprocessableContent());
        enviar(post("/api/mascotas/" + mascota + "/transferencia"), token, """
                {"nuevoPropietarioId":"%s"}""".formatted(anterior)).andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu019_reportePorEspecieYRaza() throws Exception {
        String propietario = crearPropietario("109", "María Gómez");
        for (int i = 0; i < 3; i++) {
            crearMascota(propietario, "Canino", "Criollo");
        }
        for (int i = 0; i < 2; i++) {
            crearMascota(propietario, "Felino", "Persa");
        }

        enviar(get("/api/reportes/mascotas"), tokenDe("ADMINISTRADOR"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].especie").value("Canino"))
                .andExpect(jsonPath("$[0].raza").value("Criollo"))
                .andExpect(jsonPath("$[0].total").value(3))
                .andExpect(jsonPath("$[1].especie").value("Felino"))
                .andExpect(jsonPath("$[1].total").value(2));
    }
}
